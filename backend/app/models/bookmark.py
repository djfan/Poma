from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Float
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

Base = declarative_base()

class Bookmark(Base):
    __tablename__ = "bookmarks"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # 播客信息
    podcast_name = Column(String, nullable=False)
    episode_name = Column(String, nullable=False)
    spotify_episode_id = Column(String, nullable=True)
    podcast_cover_url = Column(String, nullable=True)
    
    # 时间戳信息
    timestamp_ms = Column(Integer, nullable=False)  # 毫秒级时间戳
    duration_ms = Column(Integer, nullable=True)    # 总时长
    
    # 笔记内容
    audio_file_path = Column(String, nullable=True)  # 语音文件路径
    transcript_text = Column(Text, nullable=True)    # 转录文本
    user_note = Column(Text, nullable=True)          # 用户手动笔记
    
    # AI 增强内容
    ai_summary = Column(Text, nullable=True)         # AI 总结
    context_before = Column(Text, nullable=True)     # 前30秒上下文
    context_after = Column(Text, nullable=True)      # 后30秒上下文
    
    # 元数据
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    # 关系
    user = relationship("User", back_populates="bookmarks")