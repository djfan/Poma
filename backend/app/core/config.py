from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    # 数据库配置
    DATABASE_URL: str = "postgresql://user:password@localhost/poma"
    
    # Redis 配置
    REDIS_URL: str = "redis://localhost:6379"
    
    # JWT 配置
    SECRET_KEY: str = "your-secret-key-here"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    
    # Spotify API 配置
    SPOTIFY_CLIENT_ID: Optional[str] = None
    SPOTIFY_CLIENT_SECRET: Optional[str] = None
    SPOTIFY_REDIRECT_URI: str = "http://localhost:8000/api/v1/auth/spotify/callback"
    
    # OpenAI 配置
    OPENAI_API_KEY: Optional[str] = None
    
    # Google OAuth 配置
    GOOGLE_CLIENT_ID: Optional[str] = None
    
    # 应用配置
    DEBUG: bool = True
    API_V1_STR: str = "/api/v1"
    
    class Config:
        env_file = ".env"

settings = Settings()