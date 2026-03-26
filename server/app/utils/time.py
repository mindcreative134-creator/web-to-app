from datetime import datetime, timezone

def utcnow():
    """Returns the current UTC time in a timezone-aware format."""
    return datetime.now(timezone.utc)
