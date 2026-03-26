from sqlalchemy import Column, Integer, String, Boolean, DateTime, BigInteger, Text
from app.database import Base
from app.utils.time import utcnow

class Announcement(Base):
    __tablename__ = "announcements"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    title = Column(String(200))
    content = Column(Text)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=utcnow)
