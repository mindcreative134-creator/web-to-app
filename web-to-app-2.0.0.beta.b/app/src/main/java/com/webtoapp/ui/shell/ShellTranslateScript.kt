package com.webtoapp.ui.shell

/**
 * 注入网页自动翻译脚本
 * 使用Native桥接调用Google Translate API，避免CORS限制
 */
internal fun injectTranslateScript(webView: android.webkit.WebView, targetLanguage: String, showButton: Boolean) {
    val translateScript = """
        (function() {
            if (window._translateInjected) return;
            window._translateInjected = true;
            
            var targetLang = '$targetLanguage';
            var showBtn = $showButton;
            var pendingCallbacks = {};
            var callbackIdCounter = 0;
            
            // Native翻译回调处理
            window._translateCallback = function(callbackId, resultsJson, error) {
                var cb = pendingCallbacks[callbackId];
                if (cb) {
                    delete pendingCallbacks[callbackId];
                    if (error) {
                        cb.reject(error);
                    } else {
                        try {
                            cb.resolve(JSON.parse(resultsJson));
                        } catch(e) {
                            cb.reject(e.message);
                        }
                    }
                }
            };
            
            // 调用Native翻译
            function nativeTranslate(texts) {
                return new Promise(function(resolve, reject) {
                    var callbackId = 'cb_' + (++callbackIdCounter);
                    pendingCallbacks[callbackId] = { resolve: resolve, reject: reject };
                    
                    if (window._nativeTranslate && window._nativeTranslate.translate) {
                        window._nativeTranslate.translate(JSON.stringify(texts), targetLang, callbackId);
                    } else {
                        // 降级：使用fetch（可能有CORS问题）
                        fallbackTranslate(texts, callbackId);
                    }
                });
            }
            
            // 降级翻译方案
            function fallbackTranslate(texts, callbackId) {
                var url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + targetLang + '&dt=t&q=' + encodeURIComponent(texts.join('\n'));
                fetch(url)
                    .then(function(r) { return r.json(); })
                    .then(function(data) {
                        if (data && data[0]) {
                            var translations = data[0].map(function(item) { return item[0]; });
                            var combined = translations.join('').split('\n');
                            window._translateCallback(callbackId, JSON.stringify(combined), null);
                        } else {
                            window._translateCallback(callbackId, null, 'Invalid response');
                        }
                    })
                    .catch(function(e) {
                        window._translateCallback(callbackId, null, e.message);
                    });
            }
            
            // Create翻译按钮
            if (showBtn) {
                var btn = document.createElement('div');
                btn.id = '_translate_btn';
                btn.innerHTML = 'Translate';
                btn.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:999999;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#fff;padding:12px 20px;border-radius:25px;font-size:14px;font-weight:bold;cursor:pointer;box-shadow:0 4px 15px rgba(102,126,234,0.4);transition:all 0.3s ease;';
                btn.onclick = function() { translatePage(); };
                document.body.appendChild(btn);
            }
            
            // 翻译页面函数
            async function translatePage() {
                var texts = [];
                var elements = [];
                
                // 收集需要翻译的文本节点
                var walker = document.createTreeWalker(
                    document.body,
                    NodeFilter.SHOW_TEXT,
                    { acceptNode: function(node) {
                        var parent = node.parentNode;
                        if (!parent) return NodeFilter.FILTER_REJECT;
                        var tag = parent.tagName;
                        if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT') return NodeFilter.FILTER_REJECT;
                        var text = node.textContent.trim();
                        if (text.length < 2) return NodeFilter.FILTER_REJECT;
                        if (/^[\s\d\p{P}]+$/u.test(text)) return NodeFilter.FILTER_REJECT;
                        return NodeFilter.FILTER_ACCEPT;
                    }}
                );
                
                while (walker.nextNode()) {
                    var text = walker.currentNode.textContent.trim();
                    if (text && texts.indexOf(text) === -1) {
                        texts.push(text);
                        elements.push(walker.currentNode);
                    }
                }
                
                if (texts.length === 0) return;
                
                // Update按钮状态
                if (showBtn) {
                    var btn = document.getElementById('_translate_btn');
                    if (btn) btn.innerHTML = 'Translating...';
                }
                
                // 分批翻译
                var batchSize = 20;
                for (var i = 0; i < texts.length; i += batchSize) {
                    var batch = texts.slice(i, i + batchSize);
                    var batchElements = elements.slice(i, i + batchSize);
                    
                    try {
                        var results = await nativeTranslate(batch);
                        for (var j = 0; j < batchElements.length && j < results.length; j++) {
                            if (results[j] && results[j].trim()) {
                                batchElements[j].textContent = results[j];
                            }
                        }
                    } catch(e) {
                        console.log('Translate batch error:', e);
                    }
                }
                
                if (showBtn) {
                    var btn = document.getElementById('_translate_btn');
                    if (btn) btn.innerHTML = 'Translated';
                }
            }
            
            // Auto翻译
            setTimeout(translatePage, 1500);
        })();
    """.trimIndent()
    
    webView.evaluateJavascript(translateScript, null)
}
