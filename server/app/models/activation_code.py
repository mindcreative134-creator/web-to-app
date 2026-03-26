from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, BigInteger
from sqlalchemy.orm import relationship
from app.database import Base
from app.utils.time import utcnow

class ActivationCode(Base):
    __tablename__ = "activation_codes"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    code = Column(String(100), unique=True, index=True)
    plan_type = Column(String(50))
    status = Column(String(20), default="unused") # unused, used, expired
    used_by_id = Column(BigInteger, ForeignKey("users.id"))
    created_at = Column(DateTime, default=utcnow)
    used_at = Column(DateTime)

class ProTransaction(Base):
    __tablename__ = "pro_transactions"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"))
    type = Column(String(50))
    plan_type = Column(String(50))
    pro_start = Column(DateTime)
    note = Column(String(200))
    activation_code = Column(String(100))
    created_at = Column(DateTime, default=utcnow)
    
    user = relationship("User", back_populates="pro_transactions")
