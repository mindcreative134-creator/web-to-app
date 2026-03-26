from datetime import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, BigInteger, Text, Float, JSON
from sqlalchemy.orm import relationship
from app.database import Base
from app.utils.time import utcnow

class StoreModule(Base):
    __tablename__ = "store_modules"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    author_id = Column(BigInteger, ForeignKey("users.id", ondelete="CASCADE"))
    name = Column(String(100), unique=True, index=True)
    title = Column(String(200))
    module_type = Column(String(50))
    description = Column(Text)
    is_approved = Column(Boolean, default=False)
    is_featured = Column(Boolean, default=False)
    downloads = Column(Integer, default=0)
    view_count = Column(Integer, default=0)
    like_count = Column(Integer, default=0)
    rating = Column(Float, default=0.0)
    rating_count = Column(Integer, default=0)
    comment_count = Column(Integer, default=0)
    
    icon = Column(String(500))
    category = Column(String(50))
    tags = Column(String(200)) # Simple comma-separated tags
    version_name = Column(String(50))
    version_code = Column(Integer)
    package_name = Column(String(255))
    
    # Discovery / Download URLs
    screenshots = Column(JSON, default=[]) 
    video_url = Column(String(500))
    apk_url_github = Column(String(500))
    apk_url_gitee = Column(String(500))
    storage_url_github = Column(String(500))
    storage_url_gitee = Column(String(500))
    
    # Privacy / Support
    contact_email = Column(String(255))
    website_url = Column(String(500))
    privacy_policy_url = Column(String(500))
    file_size = Column(BigInteger, default=0)

    created_at = Column(DateTime, default=utcnow)
    updated_at = Column(DateTime, default=utcnow, onupdate=utcnow)
    
    author = relationship("User")

class ModuleComment(Base):
    __tablename__ = "module_comments"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    module_id = Column(BigInteger, ForeignKey("store_modules.id", ondelete="CASCADE"))
    user_id = Column(BigInteger, ForeignKey("users.id"))
    content = Column(Text)
    is_deleted = Column(Boolean, default=False)
    created_at = Column(DateTime, default=utcnow)
