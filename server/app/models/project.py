from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, BigInteger, Text
from sqlalchemy.orm import relationship
from app.database import Base
from app.utils.time import utcnow

class Project(Base):
    __tablename__ = "projects"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    owner_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"), index=True)
    project_name = Column(String(100))
    project_key = Column(String(100), unique=True, index=True)
    package_name = Column(String(255))
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=utcnow)
    updated_at = Column(DateTime, default=utcnow, onupdate=utcnow)
    
    owner = relationship("User", back_populates="projects")

class ProjectActivationCode(Base):
    __tablename__ = "project_activation_codes"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    project_id = Column(BigInteger, ForeignKey("projects.id", ondelete="CASCADE"))
    code = Column(String(100), unique=True)
    created_at = Column(DateTime, default=utcnow)

class ProjectVersion(Base):
    __tablename__ = "project_versions"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    project_id = Column(BigInteger, ForeignKey("projects.id", ondelete="CASCADE"))
    version_name = Column(String(50))
    version_code = Column(Integer)
    created_at = Column(DateTime, default=utcnow)
