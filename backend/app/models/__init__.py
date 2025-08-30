"""Database models for Poma API.

All models import from a single Base to ensure consistency.
"""
from app.core.database import Base

# Import all models to ensure they are registered with SQLAlchemy
from .user import User
from .bookmark import Bookmark

__all__ = ["Base", "User", "Bookmark"]