from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, BigInteger, Text
from sqlalchemy.orm import relationship
from app.database import Base
from app.utils.time import utcnow

class ModuleReport(Base):
    __tablename__ = "module_reports"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    reporter_id = Column(BigInteger, ForeignKey("users.id"))
    module_id = Column(BigInteger, ForeignKey("store_modules.id"), nullable=True)
    comment_id = Column(BigInteger, ForeignKey("module_comments.id"), nullable=True)
    reason = Column(String(100))
    description = Column(Text)
    status = Column(String(20), default="pending")
    reviewed_at = Column(DateTime)
    reviewed_by = Column(BigInteger, ForeignKey("users.id"))
    resolution_note = Column(Text)
    created_at = Column(DateTime, default=utcnow)
