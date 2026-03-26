package com.webtoapp.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ad Blocker — production-grade ABP/uBlock-compatible filter engine
 *
 * Supports:
 * - ABP / EasyList / AdGuard network filter syntax (||, @@, $modifiers)
 * - Cosmetic element hiding (## / #@#)
 * - Resource-type–aware blocking ($script, $image, $stylesheet, …)
 * - $third-party / $first-party / $domain modifiers
 * - Exception rules (@@) to prevent page breakage
 * - Anti-anti-adblock scriptlet injection (abort-on-property-read, etc.)
 * - Comprehensive safelist for critical infrastructure domains
 * - Hosts file import (standard, AdGuard DNS, ABP)
 */
class AdBlocker {

    // ==================== Resource type enum ====================
    enum class ResourceType {
        DOCUMENT, SUBDOCUMENT, SCRIPT, STYLESHEET, IMAGE, FONT,
        XMLHTTPREQUEST, MEDIA, WEBSOCKET, OBJECT, PING, OTHER;

        companion object {
            fun fromWebViewType(type: String): ResourceType = when (type) {
                "main_frame" -> DOCUMENT
                "sub_frame" -> SUBDOCUMENT
                "script" -> SCRIPT
                "stylesheet" -> STYLESHEET
                "image" -> IMAGE
                "font" -> FONT
                "xmlhttprequest" -> XMLHTTPREQUEST
                "media" -> MEDIA
                "websocket" -> WEBSOCKET
                "object" -> OBJECT
                "ping" -> PING
                else -> OTHER
            }
        }
    }

    // ==================== Parsed filter data classes ====================
    /**
     * A parsed network filter rule.
     * Compiled once, matched many times — hot path is simple string/set operations.
     */
    private data class NetworkFilter(
        val pattern: String,
        val regex: Regex?,
        val isException: Boolean,
        val matchCase: Boolean,
        // Domain constraints: block only on these domains (empty = all)
        val domains: Set<String>,
        val excludedDomains: Set<String>,
        // Resource type constraints
        val allowedTypes: Set<ResourceType>?,   // null = all types
        val excludedTypes: Set<ResourceType>,
        // Party constraints
        val thirdPartyOnly: Boolean,
        val firstPartyOnly: Boolean,
        // Raw host anchor for fast O(1) pre-check (||domain^ → "domain")
        val anchorDomain: String?
    )

    /**
     * A parsed cosmetic filter (element hiding rule).
     */
    data class CosmeticFilter(
        val selector: String,
        val isException: Boolean,
        val domains: Set<String>,       // apply only on these domains (empty = all)
        val excludedDomains: Set<String>
    )

    companion object {
        private val IP_ADDRESS_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        private val HOST_EXTRACT_REGEX = Regex("^(?:https?://)?([^/:]+)")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val ABP_SEPARATOR_REGEX = Regex("[^\\w%.\\-]")

        // Critical infrastructure — NEVER block these or pages will break
        private val SAFELIST_HOSTS = setOf(
            // Translation
            "translate.googleapis.com", "translate.google.com", "translation.googleapis.com",
            // CDNs serving first-party content
            "cdn.jsdelivr.net", "cdnjs.cloudflare.com", "unpkg.com",
            "ajax.googleapis.com", "fonts.googleapis.com", "fonts.gstatic.com",
            // Login / OAuth
            "accounts.google.com", "login.microsoftonline.com", "appleid.apple.com",
            "github.com", "api.github.com",
            // Payment
            "js.stripe.com", "checkout.stripe.com", "www.paypal.com",
            "pay.google.com", "applepay.cdn-apple.com",
            // reCAPTCHA / hCaptcha
            "www.google.com", "www.gstatic.com",
            "hcaptcha.com", "js.hcaptcha.com",
            "challenges.cloudflare.com",
            // Map tiles
            "maps.googleapis.com", "maps.gstatic.com",
            "api.mapbox.com", "tiles.mapbox.com"
        )

        // Popular hosts file / filter list sources
        fun getPopularHostsSources() = listOf(
            HostsSource(
                name = "AdGuard DNS Filter",
                url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAdGuardDesc
            ),
            HostsSource(
                name = "StevenBlack Hosts",
                url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                description = com.webtoapp.core.i18n.Strings.hostsStevenBlackDesc
            ),
            HostsSource(
                name = "AdAway Default",
                url = "https://adaway.org/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAdAwayDesc
            ),
            HostsSource(
                name = "Anti-AD",
                url = "https://anti-ad.net/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAntiADDesc
            ),
            HostsSource(
                name = "1Hosts Lite",
                url = "https://o0.pages.dev/Lite/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hosts1HostsLiteDesc
            ),
            HostsSource(
                name = "EasyList",
                url = "https://easylist.to/easylist/easylist.txt",
                description = "EasyList — primary filter list for international ads"
            ),
            HostsSource(
                name = "EasyPrivacy",
                url = "https://easylist.to/easylist/easyprivacy.txt",
                description = "EasyPrivacy — comprehensive tracker blocking"
            ),
            HostsSource(
                name = "EasyList China",
                url = "https://easylist-downloads.adblockplus.org/easylistchina.txt",
                description = "EasyList China — Chinese site ad filters"
            ),
            HostsSource(
                name = "uBlock Filters",
                url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                description = "uBlock Origin default filters"
            ),
            HostsSource(
                name = "Peter Lowe's List",
                url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0",
                description = "Peter Lowe's ad & tracking server list"
            )
        )

        @Deprecated("Use getPopularHostsSources() instead for i18n support")
        val POPULAR_HOSTS_SOURCES get() = getPopularHostsSources()

        // ==================== Comprehensive default ad/tracker domains ====================
        private val DEFAULT_AD_HOSTS = setOf(
            // Google Ads
            "googleadservices.com", "googlesyndication.com", "doubleclick.net",
            "googletagmanager.com", "googletagservices.com",
            "pagead2.googlesyndication.com", "adservice.google.com",
            "afs.googlesyndication.com", "partner.googleadservices.com",
            // Facebook / Meta
            "an.facebook.com", "pixel.facebook.com",
            // Major ad exchanges
            "adnxs.com", "advertising.com", "taboola.com", "outbrain.com",
            "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com",
            "casalemedia.com", "openx.net", "bidswitch.net", "adsrvr.org",
            "sharethrough.com", "33across.com", "media.net",
            "amazon-adsystem.com", "serving-sys.com", "smartadserver.com",
            "contextweb.com", "yieldmo.com", "sovrn.com",
            "liadm.com", "lijit.com", "revcontent.com",
            "zedo.com", "undertone.com", "conversantmedia.com",
            "indexexchange.com", "spotxchange.com", "districtm.io",
            "triplelift.com", "gumgum.com", "nativo.com",
            // Tracking / analytics (ad-serving related)
            "moatads.com", "doubleverify.com", "adsafeprotected.com",
            "scorecardresearch.com", "imrworldwide.com", "quantserve.com",
            "demdex.net", "krxd.net", "exelator.com", "bluekai.com",
            "eyeota.net", "rlcdn.com", "pippio.com",
            "addthis.com", "sharethis.com",
            // Pop-ups / redirectors
            "popads.net", "popcash.net", "propellerads.com",
            "clickadu.com", "trafficjunky.com", "exoclick.com",
            "juicyads.com", "plugrush.com", "hilltopads.com",
            // Chinese ad networks
            "cpro.baidu.com", "pos.baidu.com", "cbjs.baidu.com",
            "eclick.baidu.com", "hm.baidu.com",
            "tanx.com", "alimama.com", "mmstat.com",
            "cnzz.com", "51.la",
            "union.sogou.com", "js.sogou.com",
            "c.360.cn", "g.360.cn",
            "s.360.cn", "stat.360.cn",
            "miaozhen.com", "admaster.com.cn",
            "gridsumdissector.com", "cnzz.com",
            "mediav.com", "ipinyou.com",
            "yigao.com", "adview.cn",
            "admon.cn", "allyes.com",
            "admaimai.com", "adsame.com",
            "aduu.cn", "baidustatic.com"
        )

        // Resource type modifier name → enum
        private val TYPE_MODIFIERS = mapOf(
            "script" to ResourceType.SCRIPT,
            "stylesheet" to ResourceType.STYLESHEET,
            "css" to ResourceType.STYLESHEET,
            "image" to ResourceType.IMAGE,
            "font" to ResourceType.FONT,
            "xmlhttprequest" to ResourceType.XMLHTTPREQUEST,
            "xhr" to ResourceType.XMLHTTPREQUEST,
            "media" to ResourceType.MEDIA,
            "websocket" to ResourceType.WEBSOCKET,
            "object" to ResourceType.OBJECT,
            "subdocument" to ResourceType.SUBDOCUMENT,
            "sub_frame" to ResourceType.SUBDOCUMENT,
            "document" to ResourceType.DOCUMENT,
            "ping" to ResourceType.PING,
            "other" to ResourceType.OTHER
        )

        // 预分配的阻止响应字节数组 (减少 GC 压力)
        // 每页可能阻止 30-100 个广告请求, 预分配避免重复创建 ByteArray
        val EMPTY_BYTES = ByteArray(0)
        val BLOCKED_JS_BYTES = "/* blocked */".toByteArray()
        val BLOCKED_CSS_BYTES = "/* blocked */".toByteArray()
        val BLOCKED_JSON_BYTES = "{}".toByteArray()
    }

    // ==================== Storage ====================
    // Fast domain lookup sets
    private val exactHosts = mutableSetOf<String>()
    private val hostsFileHosts = mutableSetOf<String>()
    private val enabledHostsSources = mutableSetOf<String>()

    // Parsed ABP filters — split for performance
    private val networkBlockFilters = mutableListOf<NetworkFilter>()
    private val networkExceptionFilters = mutableListOf<NetworkFilter>()
    // Anchor-domain index: domain → list of filter indices — O(1) lookup
    private val anchorDomainIndex = HashMap<String, MutableList<Int>>()

    // Cosmetic filters
    private val cosmeticBlockFilters = mutableListOf<CosmeticFilter>()
    private val cosmeticExceptionFilters = mutableListOf<CosmeticFilter>()

    // Anti-anti-adblock scriptlets
    private val scriptletRules = mutableListOf<Pair<Set<String>, String>>() // domains → scriptlet call

    private var enabled = false

    // ==================== Public API ====================

    fun setEnabled(enable: Boolean) { enabled = enable }
    fun isEnabled(): Boolean = enabled

    /**
     * Initialize blocker with custom rules and optional defaults.
     */
    fun initialize(customRules: List<String> = emptyList(), useDefaultRules: Boolean = true) {
        exactHosts.clear()
        networkBlockFilters.clear()
        networkExceptionFilters.clear()
        anchorDomainIndex.clear()
        cosmeticBlockFilters.clear()
        cosmeticExceptionFilters.clear()
        scriptletRules.clear()

        if (useDefaultRules) {
            exactHosts.addAll(DEFAULT_AD_HOSTS)
        }

        customRules.forEach { parseAndAddRule(it) }
    }

    /**
     * Full-featured blocking check.
     *
     * @param url             Request URL
     * @param pageHost        Host of the top-level page (for $third-party / $domain)
     * @param resourceType    Resource type string from WebView (e.g. "script", "image")
     * @param isThirdParty    Whether the request is third-party
     * @return true if the request should be blocked
     */
    fun shouldBlock(
        url: String,
        pageHost: String? = null,
        resourceType: String? = null,
        isThirdParty: Boolean = true
    ): Boolean {
        if (!enabled) return false

        val lowerUrl = url.lowercase()
        val urlHost = extractHost(lowerUrl)

        // Safelist — critical infrastructure never blocked (HashSet O(1))
        if (urlHost != null && matchesHostSet(urlHost, SAFELIST_HOSTS)) return false

        val resType = resourceType?.let { ResourceType.fromWebViewType(it) } ?: ResourceType.OTHER

        // 优化顺序: 先做超快的 HashSet O(1) 检查，再做耗时的 Regex 过滤器扫描
        // Phase 1: Exact host match (HashSet O(1) — 最快检查)
        val hostMatched = urlHost != null && (matchesHostSet(urlHost, exactHosts) || matchesHostSet(urlHost, hostsFileHosts))

        if (hostMatched) {
            // 仅在匹配时才检查异常过滤器 — 避免无谓扫描
            if (matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkExceptionFilters)) {
                return false
            }
            return true
        }

        // Phase 2: Check exception filters (only if not already matched by exact host)
        if (matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkExceptionFilters)) {
            return false
        }

        // Phase 3: ABP network filters (block rules — regex scan)
        if (matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkBlockFilters)) {
            return true
        }

        return false
    }

    // Backward-compatible overload (existing call sites)
    fun shouldBlock(request: WebResourceRequest): Boolean {
        return shouldBlock(request.url.toString())
    }

    /**
     * Get cosmetic CSS selectors to hide on a given page.
     * Returns a CSS string ready for injection as a <style> element.
     */
    fun getCosmeticFilterCss(pageHost: String): String {
        if (!enabled) return ""

        // Collect exception selectors for this page
        val exceptionSelectors = cosmeticExceptionFilters
            .filter { matchesCosmeticDomain(it, pageHost) }
            .map { it.selector }
            .toSet()

        // Collect block selectors, minus exceptions
        val selectors = cosmeticBlockFilters
            .filter { matchesCosmeticDomain(it, pageHost) && it.selector !in exceptionSelectors }
            .map { it.selector }
            .distinct()

        if (selectors.isEmpty()) return ""

        // Batch selectors (avoid CSS selector list length limits in older WebViews)
        val batchSize = 50
        return selectors.chunked(batchSize).joinToString("\n") { batch ->
            batch.joinToString(",\n") + " { display: none !important; visibility: hidden !important; height: 0 !important; min-height: 0 !important; overflow: hidden !important; }"
        }
    }

    /**
     * Generate anti-anti-adblock scriptlet JS for a given page.
     */
    fun getAntiAdblockScript(pageHost: String): String {
        if (!enabled) return ""

        val applicableScriptlets = scriptletRules.filter { (domains, _) ->
            domains.isEmpty() || domains.any { d ->
                pageHost == d || pageHost.endsWith(".$d")
            }
        }
        if (applicableScriptlets.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("(function(){")
        sb.appendLine("'use strict';")
        sb.appendLine("if(window.__wta_aab_injected__)return;window.__wta_aab_injected__=true;")

        applicableScriptlets.forEach { (_, scriptlet) ->
            sb.appendLine(scriptlet)
        }

        // Built-in universal anti-anti-adblock measures
        sb.appendLine(UNIVERSAL_ANTI_ADBLOCK_SCRIPT)

        sb.appendLine("})();")
        return sb.toString()
    }


    /**
     * Return empty response (used to block ad requests)
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(EMPTY_BYTES))
    }

    /**
     * Create a type-specific empty response to avoid page errors
     * 使用预分配的字节数组, 仅创建 InputStream 包装器 (最小 GC 压力)
     */
    fun createEmptyResponse(resourceType: String): WebResourceResponse {
        return when (resourceType) {
            "script" -> WebResourceResponse("application/javascript", "UTF-8",
                ByteArrayInputStream(BLOCKED_JS_BYTES))
            "stylesheet" -> WebResourceResponse("text/css", "UTF-8",
                ByteArrayInputStream(BLOCKED_CSS_BYTES))
            "image" -> WebResourceResponse("image/gif", null,
                ByteArrayInputStream(TRANSPARENT_GIF))
            "xmlhttprequest" -> WebResourceResponse("application/json", "UTF-8",
                ByteArrayInputStream(BLOCKED_JSON_BYTES))
            else -> WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(EMPTY_BYTES))
        }
    }

    // ==================== Rule counts ====================
    fun getRuleCount(): Int = exactHosts.size + hostsFileHosts.size +
        networkBlockFilters.size + networkExceptionFilters.size +
        cosmeticBlockFilters.size + cosmeticExceptionFilters.size
    fun getHostsFileRuleCount(): Int = hostsFileHosts.size
    fun getNetworkFilterCount(): Int = networkBlockFilters.size + networkExceptionFilters.size
    fun getCosmeticFilterCount(): Int = cosmeticBlockFilters.size + cosmeticExceptionFilters.size

    fun getStats(): Map<String, Int> = mapOf(
        "exactHosts" to exactHosts.size,
        "hostsFile" to hostsFileHosts.size,
        "networkBlock" to networkBlockFilters.size,
        "networkException" to networkExceptionFilters.size,
        "cosmeticBlock" to cosmeticBlockFilters.size,
        "cosmeticException" to cosmeticExceptionFilters.size,
        "scriptlets" to scriptletRules.size
    )

    // ==================== Rule management ====================
    fun addRule(rule: String) { parseAndAddRule(rule) }

    fun removeRule(rule: String) {
        exactHosts.remove(rule)
        // For complex filter removal, re-initialize is recommended
    }

    fun clearRules() {
        exactHosts.clear()
        networkBlockFilters.clear()
        networkExceptionFilters.clear()
        anchorDomainIndex.clear()
        cosmeticBlockFilters.clear()
        cosmeticExceptionFilters.clear()
        scriptletRules.clear()
    }

    fun clearHostsFileRules() {
        hostsFileHosts.clear()
        enabledHostsSources.clear()
    }

    fun getEnabledHostsSources(): Set<String> = enabledHostsSources.toSet()
    fun isHostsSourceEnabled(url: String): Boolean = enabledHostsSources.contains(url)

    // ==================== ABP Filter Parser ====================

    /**
     * Parse a single filter rule in ABP / AdGuard / hosts format.
     */
    private fun parseAndAddRule(rawRule: String) {
        val rule = rawRule.trim()
        if (rule.isEmpty() || rule.startsWith("!") || rule.startsWith("[")) return

        // Cosmetic filter: domain##selector or domain#@#selector
        val cosmeticExIdx = rule.indexOf("#@#")
        val cosmeticIdx = if (cosmeticExIdx < 0) rule.indexOf("##") else -1

        if (cosmeticExIdx >= 0) {
            parseCosmeticFilter(rule, cosmeticExIdx, "#@#", isException = true)
            return
        }
        if (cosmeticIdx >= 0 && !rule.startsWith("||")) {
            parseCosmeticFilter(rule, cosmeticIdx, "##", isException = false)
            return
        }

        // Scriptlet: domain#%#//scriptlet(...)
        if (rule.contains("#%#//scriptlet(")) {
            parseScriptletRule(rule)
            return
        }
        // AG scriptlet shorthand: domain##+js(...)
        if (rule.contains("##+js(")) {
            parseScriptletRule(rule)
            return
        }

        // Network filter
        parseNetworkFilter(rule)
    }

    private fun parseCosmeticFilter(rule: String, idx: Int, delimiter: String, isException: Boolean) {
        val domainPart = rule.substring(0, idx)
        val selector = rule.substring(idx + delimiter.length).trim()
        if (selector.isEmpty()) return

        val (domains, excludedDomains) = parseDomainList(domainPart)

        val filter = CosmeticFilter(
            selector = selector,
            isException = isException,
            domains = domains,
            excludedDomains = excludedDomains
        )

        if (isException) cosmeticExceptionFilters.add(filter)
        else cosmeticBlockFilters.add(filter)
    }

    private fun parseScriptletRule(rule: String) {
        val domainEnd = rule.indexOfFirst { it == '#' }
        val domainPart = if (domainEnd > 0) rule.substring(0, domainEnd) else ""
        val (domains, _) = parseDomainList(domainPart)

        // Extract scriptlet name & args
        val scriptletMatch = Regex("(?:scriptlet|js)\\((.+)\\)").find(rule) ?: return
        val args = scriptletMatch.groupValues[1].split(",").map { it.trim().removeSurrounding("'").removeSurrounding("\"") }
        if (args.isEmpty()) return

        val js = generateScriptlet(args[0], args.drop(1))
        if (js.isNotEmpty()) {
            scriptletRules.add(domains to js)
        }
    }

    private fun parseNetworkFilter(rule: String) {
        var raw = rule
        val isException = raw.startsWith("@@")
        if (isException) raw = raw.removePrefix("@@")

        // Split off $ modifiers
        val dollarIdx = raw.lastIndexOf('$')
        var patternPart = raw
        var modifierPart = ""
        if (dollarIdx > 0) {
            // Make sure $ is not inside a regex
            val beforeDollar = raw.substring(0, dollarIdx)
            if (!beforeDollar.contains('/') || beforeDollar.count { it == '/' } % 2 == 0) {
                patternPart = beforeDollar
                modifierPart = raw.substring(dollarIdx + 1)
            }
        }

        // Parse modifiers
        var thirdPartyOnly = false
        var firstPartyOnly = false
        var matchCase = false
        val domainConstraints = mutableSetOf<String>()
        val excludedDomainConstraints = mutableSetOf<String>()
        val allowedTypes = mutableSetOf<ResourceType>()
        val excludedTypes = mutableSetOf<ResourceType>()

        if (modifierPart.isNotEmpty()) {
            modifierPart.split(",").forEach { mod ->
                val m = mod.trim().lowercase()
                when {
                    m == "third-party" || m == "3p" -> thirdPartyOnly = true
                    m == "~third-party" || m == "first-party" || m == "1p" -> firstPartyOnly = true
                    m == "match-case" -> matchCase = true
                    m == "important" -> { /* priority — we already prioritize exceptions */ }
                    m.startsWith("domain=") -> {
                        m.removePrefix("domain=").split("|").forEach { d ->
                            if (d.startsWith("~")) excludedDomainConstraints.add(d.removePrefix("~"))
                            else domainConstraints.add(d)
                        }
                    }
                    m.startsWith("~") -> {
                        TYPE_MODIFIERS[m.removePrefix("~")]?.let { excludedTypes.add(it) }
                    }
                    else -> {
                        TYPE_MODIFIERS[m]?.let { allowedTypes.add(it) }
                    }
                }
            }
        }

        // Detect anchor domain: ||domain^ or ||domain/
        var anchorDomain: String? = null
        if (patternPart.startsWith("||")) {
            val domainEnd = patternPart.indexOfFirst { it == '^' || it == '/' || it == '*' || it == '$' }
            anchorDomain = if (domainEnd > 2) patternPart.substring(2, domainEnd).lowercase()
            else patternPart.removePrefix("||").removeSuffix("^").removeSuffix("$").lowercase()
        }

        // Simple host-only rule: ||domain^ with no modifiers → fast path
        if (anchorDomain != null && !anchorDomain.contains('*') &&
            (patternPart == "||$anchorDomain^" || patternPart == "||$anchorDomain" ||
             patternPart == "||$anchorDomain^|") &&
            modifierPart.isEmpty() && !isException) {
            exactHosts.add(anchorDomain)
            return
        }

        // Compile pattern to regex
        val regex = compileAbpPattern(patternPart, matchCase)

        val filter = NetworkFilter(
            pattern = patternPart,
            regex = regex,
            isException = isException,
            matchCase = matchCase,
            domains = domainConstraints,
            excludedDomains = excludedDomainConstraints,
            allowedTypes = allowedTypes.ifEmpty { null },
            excludedTypes = excludedTypes,
            thirdPartyOnly = thirdPartyOnly,
            firstPartyOnly = firstPartyOnly,
            anchorDomain = anchorDomain
        )

        if (isException) {
            networkExceptionFilters.add(filter)
        } else {
            val idx = networkBlockFilters.size
            networkBlockFilters.add(filter)
            // Index by anchor domain for fast lookup
            if (anchorDomain != null && !anchorDomain.contains('*')) {
                anchorDomainIndex.getOrPut(anchorDomain) { mutableListOf() }.add(idx)
            }
        }
    }

    /**
     * Compile ABP filter pattern to Regex.
     * ||  = domain anchor
     * |   = string start/end anchor
     * ^   = separator (anything except alphanumeric, %, -, .)
     * *   = wildcard
     */
    private fun compileAbpPattern(pattern: String, matchCase: Boolean): Regex? {
        if (pattern.isEmpty()) return null
        return try {
            var p = pattern
            // Regex filter: /regex/
            if (p.startsWith("/") && p.endsWith("/") && p.length > 2) {
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                return Regex(p.substring(1, p.length - 1), options)
            }

            // Escape regex special chars (except ABP special chars we handle)
            p = p.replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")

            // ABP special: || → domain anchor
            p = p.replace("||", "^(?:https?|wss?)://(?:[^/]*\\.)?")
            // ABP special: | at start → string start
            if (p.startsWith("|")) p = "^" + p.removePrefix("|")
            // ABP special: | at end → string end
            if (p.endsWith("|")) p = p.removeSuffix("|") + "$"
            // ABP special: ^ → separator
            p = p.replace("^", "[^\\w%.\\-]")
            // ABP special: * → wildcard
            p = p.replace("*", ".*")

            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            Regex(p, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDomainList(domainStr: String): Pair<Set<String>, Set<String>> {
        if (domainStr.isBlank()) return emptySet<String>() to emptySet()
        val include = mutableSetOf<String>()
        val exclude = mutableSetOf<String>()
        domainStr.split(",").forEach { d ->
            val trimmed = d.trim().lowercase()
            if (trimmed.startsWith("~")) exclude.add(trimmed.removePrefix("~"))
            else if (trimmed.isNotEmpty()) include.add(trimmed)
        }
        return include to exclude
    }

    // ==================== Network filter matching ====================

    private fun matchesAnyNetworkFilter(
        url: String,
        urlHost: String?,
        pageHost: String?,
        resType: ResourceType,
        isThirdParty: Boolean,
        filters: List<NetworkFilter>
    ): Boolean {
        // Fast path: check anchor domain index first
        if (urlHost != null && filters === networkBlockFilters) {
            var domain: String = urlHost
            while (domain.contains('.')) {
                anchorDomainIndex[domain]?.forEach { idx ->
                    val filter = networkBlockFilters[idx]
                    if (matchesNetworkFilter(filter, url, urlHost, pageHost, resType, isThirdParty)) {
                        return true
                    }
                }
                domain = domain.substringAfter('.')
            }
            anchorDomainIndex[domain]?.forEach { idx ->
                val filter = networkBlockFilters[idx]
                if (matchesNetworkFilter(filter, url, urlHost, pageHost, resType, isThirdParty)) {
                    return true
                }
            }
        }

        // Full scan for non-indexed filters and exception filters
        for (filter in filters) {
            if (filter.anchorDomain != null && filters === networkBlockFilters) continue // already checked via index
            if (matchesNetworkFilter(filter, url, urlHost, pageHost, resType, isThirdParty)) {
                return true
            }
        }
        return false
    }

    private fun matchesNetworkFilter(
        filter: NetworkFilter,
        url: String,
        urlHost: String?,
        pageHost: String?,
        resType: ResourceType,
        isThirdParty: Boolean
    ): Boolean {
        // Resource type check
        if (filter.allowedTypes != null && resType !in filter.allowedTypes) return false
        if (resType in filter.excludedTypes) return false

        // Party check
        if (filter.thirdPartyOnly && !isThirdParty) return false
        if (filter.firstPartyOnly && isThirdParty) return false

        // Domain constraint check
        if (filter.domains.isNotEmpty() && pageHost != null) {
            if (!filter.domains.any { pageHost == it || pageHost.endsWith(".$it") }) return false
        }
        if (filter.excludedDomains.isNotEmpty() && pageHost != null) {
            if (filter.excludedDomains.any { pageHost == it || pageHost.endsWith(".$it") }) return false
        }

        // Pattern match
        val regex = filter.regex ?: return false
        return regex.containsMatchIn(url)
    }

    // ==================== Cosmetic filter matching ====================

    private fun matchesCosmeticDomain(filter: CosmeticFilter, pageHost: String): Boolean {
        if (filter.excludedDomains.any { pageHost == it || pageHost.endsWith(".$it") }) return false
        if (filter.domains.isEmpty()) return true
        return filter.domains.any { pageHost == it || pageHost.endsWith(".$it") }
    }

    // ==================== Host matching ====================

    private fun matchesHostSet(host: String, hostSet: Set<String>): Boolean {
        var domain = host
        while (domain.contains('.')) {
            if (hostSet.contains(domain)) return true
            domain = domain.substringAfter('.')
        }
        return hostSet.contains(domain)
    }

    /**
     * C 级 URL host 提取 (零分配指针遍历)
     * 替换 Uri.parse(url).host 避免创建 URI 对象 + JVM GC 压力
     * shouldBlock 每次子资源请求都调用, 累计 GC 减少显著
     */
    private fun extractHost(url: String): String? {
        // C 级提取 → 回退到 Uri.parse
        val host = com.webtoapp.core.perf.NativePerfEngine.extractHost(url)
        if (host != null) return host.lowercase()
        return try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            HOST_EXTRACT_REGEX.find(url)?.groupValues?.getOrNull(1)?.lowercase()
        }
    }

    // ==================== Scriptlet generator ====================

    /**
     * Generate JS code for common anti-anti-adblock scriptlets.
     * Compatible with uBlock Origin / AdGuard scriptlet names.
     */
    private fun generateScriptlet(name: String, args: List<String>): String {
        return when (name) {
            "abort-on-property-read", "aopr" -> {
                val prop = args.getOrNull(0) ?: return ""
                """
                (function(){
                    var chain = '${prop}'.split('.');
                    var owner = window;
                    for(var i=0;i<chain.length-1;i++){
                        if(!owner[chain[i]])owner[chain[i]]={};
                        owner=owner[chain[i]];
                    }
                    var last=chain[chain.length-1];
                    Object.defineProperty(owner,last,{get:function(){throw new ReferenceError()},set:function(){}});
                })();
                """.trimIndent()
            }
            "abort-on-property-write", "aopw" -> {
                val prop = args.getOrNull(0) ?: return ""
                """
                (function(){
                    var chain='${prop}'.split('.');
                    var owner=window;
                    for(var i=0;i<chain.length-1;i++){
                        if(!owner[chain[i]])owner[chain[i]]={};
                        owner=owner[chain[i]];
                    }
                    var last=chain[chain.length-1];
                    Object.defineProperty(owner,last,{get:function(){return undefined;},set:function(){throw new Error();}});
                })();
                """.trimIndent()
            }
            "abort-current-inline-script", "acis" -> {
                val prop = args.getOrNull(0) ?: return ""
                val search = args.getOrNull(1) ?: ""
                """
                (function(){
                    var target='$prop';var needle='$search';
                    var chain=target.split('.');var owner=window;
                    for(var i=0;i<chain.length-1;i++){if(owner[chain[i]])owner=owner[chain[i]];else return;}
                    var last=chain[chain.length-1];var orig=owner[last];
                    Object.defineProperty(owner,last,{get:function(){
                        if(needle){
                            var e=new Error();if(e.stack&&e.stack.indexOf(needle)!==-1)throw new ReferenceError();
                        }else{
                            var e=new Error();var s=document.currentScript;
                            if(s&&s.textContent&&s.textContent.indexOf(target)!==-1)throw new ReferenceError();
                        }
                        return typeof orig==='function'?orig.bind(this):orig;
                    },set:function(v){orig=v;}});
                })();
                """.trimIndent()
            }
            "set-constant", "set" -> {
                val prop = args.getOrNull(0) ?: return ""
                val value = args.getOrNull(1) ?: "undefined"
                val jsValue = when(value) {
                    "true" -> "true"; "false" -> "false"
                    "null" -> "null"; "undefined" -> "undefined"
                    "noopFunc" -> "function(){}"
                    "trueFunc" -> "function(){return true;}"
                    "falseFunc" -> "function(){return false;}"
                    "emptyStr" -> "''"
                    "emptyArr" -> "[]"
                    "emptyObj" -> "{}"
                    "" -> "undefined"
                    else -> if (value.toDoubleOrNull() != null) value else "'$value'"
                }
                """
                (function(){
                    var chain='$prop'.split('.');var owner=window;
                    for(var i=0;i<chain.length-1;i++){
                        if(!owner[chain[i]])owner[chain[i]]={};
                        owner=owner[chain[i]];
                    }
                    var last=chain[chain.length-1];
                    try{Object.defineProperty(owner,last,{value:$jsValue,writable:false,configurable:true});}catch(e){/* expected */}
                })();
                """.trimIndent()
            }
            "remove-attr", "ra" -> {
                val attrs = args.getOrNull(0) ?: return ""
                val selector = args.getOrNull(1) ?: "*"
                """
                (function(){
                    var attrs='$attrs'.split('|');var sel='$selector';
                    function clean(){
                        document.querySelectorAll(sel).forEach(function(el){
                            attrs.forEach(function(a){el.removeAttribute(a);});
                        });
                    }
                    clean();
                    new MutationObserver(function(){clean();}).observe(document.documentElement,{childList:true,subtree:true,attributes:true});
                })();
                """.trimIndent()
            }
            "remove-class", "rc" -> {
                val classes = args.getOrNull(0) ?: return ""
                val selector = args.getOrNull(1) ?: "*"
                """
                (function(){
                    var cls='$classes'.split('|');var sel='$selector';
                    function clean(){
                        document.querySelectorAll(sel).forEach(function(el){
                            cls.forEach(function(c){el.classList.remove(c);});
                        });
                    }
                    clean();
                    new MutationObserver(function(){clean();}).observe(document.documentElement,{childList:true,subtree:true,attributes:true});
                })();
                """.trimIndent()
            }
            "nano-setInterval-booster", "nano-sib" -> {
                val needle = args.getOrNull(0) ?: ""
                val delay = args.getOrNull(1) ?: "1000"
                val boost = args.getOrNull(2) ?: "0.05"
                """
                (function(){
                    var needle='$needle';var delay=$delay;var boost=$boost;
                    var orig=window.setInterval;
                    window.setInterval=function(fn,ms){
                        var s=typeof fn==='function'?fn.toString():''+fn;
                        if((!needle||s.indexOf(needle)!==-1)&&ms==delay)ms=ms*boost;
                        return orig.call(this,fn,ms);
                    };
                })();
                """.trimIndent()
            }
            "nano-setTimeout-booster", "nano-stb" -> {
                val needle = args.getOrNull(0) ?: ""
                val delay = args.getOrNull(1) ?: "1000"
                val boost = args.getOrNull(2) ?: "0.05"
                """
                (function(){
                    var needle='$needle';var delay=$delay;var boost=$boost;
                    var orig=window.setTimeout;
                    window.setTimeout=function(fn,ms){
                        var s=typeof fn==='function'?fn.toString():''+fn;
                        if((!needle||s.indexOf(needle)!==-1)&&ms==delay)ms=ms*boost;
                        return orig.call(this,fn,ms);
                    };
                })();
                """.trimIndent()
            }
            "prevent-addEventListener", "aell" -> {
                val type = args.getOrNull(0) ?: ""
                val needle = args.getOrNull(1) ?: ""
                """
                (function(){
                    var type='$type';var needle='$needle';
                    var orig=EventTarget.prototype.addEventListener;
                    EventTarget.prototype.addEventListener=function(t,fn,o){
                        if(type&&t!==type)return orig.call(this,t,fn,o);
                        if(needle){var s=typeof fn==='function'?fn.toString():'';if(s.indexOf(needle)===-1)return orig.call(this,t,fn,o);}
                        return undefined;
                    };
                })();
                """.trimIndent()
            }
            "prevent-fetch", "no-fetch-if" -> {
                val needle = args.getOrNull(0) ?: ""
                """
                (function(){
                    var needle='$needle';var orig=window.fetch;
                    window.fetch=function(){
                        var url=typeof arguments[0]==='string'?arguments[0]:(arguments[0]&&arguments[0].url)||'';
                        if(!needle||url.indexOf(needle)!==-1)return Promise.resolve(new Response('',{status:200}));
                        return orig.apply(this,arguments);
                    };
                })();
                """.trimIndent()
            }
            "prevent-xhr", "no-xhr-if" -> {
                val needle = args.getOrNull(0) ?: ""
                """
                (function(){
                    var needle='$needle';var origOpen=XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open=function(m,u){
                        this.__wta_url=u;return origOpen.apply(this,arguments);
                    };
                    var origSend=XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.send=function(){
                        if(needle&&this.__wta_url&&this.__wta_url.indexOf(needle)!==-1){
                            Object.defineProperty(this,'readyState',{value:4});
                            Object.defineProperty(this,'status',{value:200});
                            Object.defineProperty(this,'responseText',{value:''});
                            Object.defineProperty(this,'response',{value:''});
                            this.dispatchEvent(new Event('load'));return;
                        }
                        return origSend.apply(this,arguments);
                    };
                })();
                """.trimIndent()
            }
            else -> ""
        }
    }

    // ==================== Hosts File Support ====================

    suspend fun importHostsFromFile(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))
            val count = inputStream.use { stream ->
                parseFilterContent(stream.bufferedReader().readText())
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importHostsFromUrl(url: String, context: Context? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Try URL content cache first (avoid re-downloading within TTL)
            var content: String? = null
            if (context != null) {
                content = AdBlockFilterCache.getCachedUrlContent(context, url)
            }
            
            if (content == null) {
                // Download from network
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "WebToApp/1.0")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(
                        Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
                    )
                }
                content = connection.inputStream.use { it.bufferedReader().readText() }
                connection.disconnect()
                
                // Cache the downloaded content
                if (context != null) {
                    AdBlockFilterCache.cacheUrlContent(context, url, content)
                }
            }
            
            val count = parseFilterContent(content)
            enabledHostsSources.add(url)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse filter list content — supports both hosts format and ABP filter syntax.
     * Auto-detects format: if lines start with [Adblock or contain ## / || / @@, treat as ABP.
     */
    private fun parseFilterContent(content: String): Int {
        var count = 0
        val isAbpFormat = content.lineSequence().take(20).any { line ->
            val t = line.trim()
            t.startsWith("[Adblock") || t.startsWith("!") || t.startsWith("||") ||
                t.startsWith("@@") || t.contains("##") || t.contains("#@#")
        }

        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[") || trimmed.startsWith("#")) {
                // For hosts format, # is comment; for ABP, lines starting with only # are ignored
                if (!trimmed.startsWith("##") && !trimmed.startsWith("#@#") && !trimmed.startsWith("#%#") && !trimmed.startsWith("##+")) {
                    return@forEach
                }
            }

            if (isAbpFormat) {
                parseAndAddRule(trimmed)
                count++
            } else {
                val host = parseHostLine(trimmed)
                if (host != null && isValidHost(host)) {
                    hostsFileHosts.add(host.lowercase())
                    count++
                }
            }
        }
        return count
    }

    private fun parseHostLine(line: String): String? {
        if (line.startsWith("||")) {
            return line.removePrefix("||").removeSuffix("^").removeSuffix("$").trim()
        }
        val parts = line.split(WHITESPACE_REGEX)
        if (parts.size >= 2) {
            val firstPart = parts[0]
            if (firstPart == "0.0.0.0" || firstPart == "127.0.0.1" ||
                firstPart.startsWith("0.") || firstPart.startsWith("127.") ||
                firstPart == "::" || firstPart == "::1") {
                val domain = parts[1].split("#")[0].trim()
                if (domain.isNotEmpty() && domain != "localhost" && domain != "localhost.localdomain") {
                    return domain
                }
            }
        }
        if (parts.size == 1 && parts[0].contains(".") && !parts[0].contains("/")) {
            return parts[0]
        }
        return null
    }

    private fun isValidHost(host: String): Boolean {
        if (host.isEmpty() || host.length > 253) return false
        if (host.startsWith(".") || host.endsWith(".")) return false
        if (host.contains("..")) return false
        if (host.matches(IP_ADDRESS_REGEX)) return false
        if (host == "localhost" || host == "broadcasthost" || host == "local") return false
        return true
    }

    suspend fun saveHostsRules(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Legacy format (backward compat)
            val file = File(context.filesDir, "adblock_hosts.txt")
            file.writeText(hostsFileHosts.joinToString("\n"))
            val sourcesFile = File(context.filesDir, "adblock_hosts_sources.txt")
            sourcesFile.writeText(enabledHostsSources.joinToString("\n"))
            
            // Save full compiled state cache
            saveCompiledStateToCache(context)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadHostsRules(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Try loading from compiled cache first (fast path)
            val cachedState = AdBlockFilterCache.loadCompiledState(context)
            if (cachedState != null) {
                restoreFromCompiledState(cachedState)
                com.webtoapp.core.logging.AppLogger.i("AdBlocker", 
                    "Restored from compiled cache: ${getRuleCount()} rules")
                return@withContext Result.success(hostsFileHosts.size)
            }
            
            // Fallback to legacy format
            val file = File(context.filesDir, "adblock_hosts.txt")
            if (file.exists()) {
                hostsFileHosts.addAll(file.readLines().filter { it.isNotBlank() })
            }
            val sourcesFile = File(context.filesDir, "adblock_hosts_sources.txt")
            if (sourcesFile.exists()) {
                enabledHostsSources.addAll(sourcesFile.readLines().filter { it.isNotBlank() })
            }
            Result.success(hostsFileHosts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Compiled State Cache Helpers ====================

    private suspend fun saveCompiledStateToCache(context: Context) {
        val blockPatterns = networkBlockFilters.map { it.toSerializable() }
        val exceptionPatterns = networkExceptionFilters.map { it.toSerializable() }

        AdBlockFilterCache.saveCompiledState(
            context = context,
            exactHosts = exactHosts.toSet(),
            hostsFileHosts = hostsFileHosts.toSet(),
            enabledSources = enabledHostsSources.toSet(),
            networkBlockPatterns = blockPatterns,
            networkExceptionPatterns = exceptionPatterns,
            cosmeticBlockFilters = cosmeticBlockFilters.toList(),
            cosmeticExceptionFilters = cosmeticExceptionFilters.toList(),
            scriptletRules = scriptletRules.toList()
        )
    }

    private fun restoreFromCompiledState(state: AdBlockFilterCache.CompiledState) {
        exactHosts.clear()
        hostsFileHosts.clear()
        enabledHostsSources.clear()
        networkBlockFilters.clear()
        networkExceptionFilters.clear()
        anchorDomainIndex.clear()
        cosmeticBlockFilters.clear()
        cosmeticExceptionFilters.clear()
        scriptletRules.clear()

        exactHosts.addAll(state.exactHosts)
        hostsFileHosts.addAll(state.hostsFileHosts)
        enabledHostsSources.addAll(state.enabledSources)

        // Recompile network filters (pattern → regex)
        state.networkBlockFilters.forEachIndexed { idx, sf ->
            val filter = sf.toNetworkFilter()
            networkBlockFilters.add(filter)
            if (filter.anchorDomain != null && !filter.anchorDomain.contains('*')) {
                anchorDomainIndex.getOrPut(filter.anchorDomain) { mutableListOf() }.add(idx)
            }
        }
        state.networkExceptionFilters.forEach { sf ->
            networkExceptionFilters.add(sf.toNetworkFilter())
        }

        cosmeticBlockFilters.addAll(state.cosmeticBlockFilters)
        cosmeticExceptionFilters.addAll(state.cosmeticExceptionFilters)
        scriptletRules.addAll(state.scriptletRules)
    }

    private fun NetworkFilter.toSerializable() = AdBlockFilterCache.SerializableNetworkFilter(
        pattern = pattern,
        isException = isException,
        matchCase = matchCase,
        domains = domains,
        excludedDomains = excludedDomains,
        allowedTypeNames = allowedTypes?.map { it.name }?.toSet(),
        excludedTypeNames = excludedTypes.map { it.name }.toSet(),
        thirdPartyOnly = thirdPartyOnly,
        firstPartyOnly = firstPartyOnly,
        anchorDomain = anchorDomain
    )

    private fun AdBlockFilterCache.SerializableNetworkFilter.toNetworkFilter() = NetworkFilter(
        pattern = pattern,
        regex = compileAbpPattern(pattern, matchCase),
        isException = isException,
        matchCase = matchCase,
        domains = domains,
        excludedDomains = excludedDomains,
        allowedTypes = allowedTypeNames?.mapNotNull { name ->
            try { ResourceType.valueOf(name) } catch (_: Exception) { null }
        }?.toSet(),
        excludedTypes = excludedTypeNames.mapNotNull { name ->
            try { ResourceType.valueOf(name) } catch (_: Exception) { null }
        }.toSet(),
        thirdPartyOnly = thirdPartyOnly,
        firstPartyOnly = firstPartyOnly,
        anchorDomain = anchorDomain
    )
}

// ==================== Constants ====================

/** 1×1 transparent GIF — returned for blocked image requests to avoid broken-image icons */
private val TRANSPARENT_GIF = byteArrayOf(
    0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
    0x00, 0x00, 0x00, 0x21, 0xF9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00,
    0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
    0x00, 0x02, 0x01, 0x00, 0x00
)

/**
 * Universal anti-anti-adblock measures.
 * These defuse the most common adblock detection patterns without targeting specific sites.
 */
private const val UNIVERSAL_ANTI_ADBLOCK_SCRIPT = """
// 1. Fake ad element — many detectors create a div with ad classes and check visibility
(function(){
    var baitClasses=['ad-banner','ad_banner','ad-placeholder','adsbox','ad-container',
        'adbadge','BannerAd','ad-large','ad-top','GoogleAd','adsense'];
    function createBait(){
        baitClasses.forEach(function(cls){
            var existing=document.querySelector('.'+cls);
            if(!existing){
                var d=document.createElement('div');
                d.className=cls;
                d.style.cssText='position:absolute!important;width:1px!important;height:1px!important;opacity:0.01!important;pointer-events:none!important;left:-9999px!important;';
                d.innerHTML='&nbsp;';
                (document.body||document.documentElement).appendChild(d);
            }
        });
    }
    if(document.readyState!=='loading')createBait();
    else document.addEventListener('DOMContentLoaded',createBait);
})();

// 2. Neutralize common adblock detection variables
(function(){
    try{
        // FuckAdBlock / BlockAdBlock
        window.fuckAdBlock=window.blockAdBlock={
            onDetected:function(){return this;},onNotDetected:function(fn){try{fn();}catch(e){/* expected */}return this;},
            on:function(_,fn){try{if(_===false||_==='notDetected')fn();}catch(e){/* expected */}return this;},
            check:function(){return false;},emitEvent:function(){return this;}
        };
        window.sniffAdBlock=window.canRunAds=true;
        window.isAdBlockActive=false;
        // AdBlock Detect 2.x
        if(typeof window.google_ad_status==='undefined')window.google_ad_status=1;
    }catch(e){/* anti-adblock defuse failed */}
})();

// 3. Prevent overlay/modal adblock walls — remove fixed/sticky overlays that appear after ads fail
(function(){
    function removeWalls(){
        var overlays=document.querySelectorAll('[class*="adblock"],[class*="ad-block"],[id*="adblock"],[id*="ad-block"],[class*="adb-overlay"],[class*="blocker-overlay"]');
        overlays.forEach(function(el){
            var style=window.getComputedStyle(el);
            if(style.position==='fixed'||style.position==='absolute'){
                el.style.display='none';
            }
        });
        // Restore scroll if locked
        if(document.body){
            var bs=window.getComputedStyle(document.body);
            if(bs.overflow==='hidden'||bs.overflowY==='hidden'){
                document.body.style.setProperty('overflow','auto','important');
            }
        }
    }
    var obs=new MutationObserver(function(){removeWalls();});
    if(document.documentElement)obs.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(function(){obs.disconnect();},15000);
})();
"""

/**
 * Hosts source info
 */
data class HostsSource(
    val name: String,
    val url: String,
    val description: String
)
