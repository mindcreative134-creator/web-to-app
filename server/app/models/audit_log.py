from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, BigInteger, JSON, Text
from app.database import Base
from app.utils.time import utcnow

class AdminAuditLog(Base):
    __tablename__ = "admin_audit_logs"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    admin_id = Column(BigInteger, ForeignKey("users.id"))
    action = Column(String(100))
    target_type = Column(String(50))
    target_id = Column(BigInteger, nullable=True)
    details = Column(JSON, nullable=True)
    ip_address = Column(String(50), nullable=True)
    created_at = Column(DateTime, default=utcnow)
