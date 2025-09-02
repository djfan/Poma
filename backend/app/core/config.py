"""Configuration for Poma API.

Explicit is better than implicit.
Simple is better than complex.
"""
from typing import Optional

from pydantic import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment."""
    
    # Core app settings
    DEBUG: bool = True
    API_V1_STR: str = "/api/v1"
    
    # Security
    SECRET_KEY: str = "dev-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440  # 24 hours
    
    # External services
    GOOGLE_CLIENT_ID: Optional[str] = None
    SPOTIFY_CLIENT_ID: Optional[str] = None
    SPOTIFY_CLIENT_SECRET: Optional[str] = None
    OPENAI_API_KEY: Optional[str] = None
    
    # Database (future)
    DATABASE_URL: str = "sqlite:///./poma.db"  # Simple default
    
    class Config:
        env_file = ".env"


settings = Settings()