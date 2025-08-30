#!/usr/bin/env python3
"""Create all database tables."""

from app.core.database import engine
from app.models.bookmark import Base as BookmarkBase
from app.models.user import Base as UserBase

def create_tables():
    """Create all tables in the database."""
    print("Creating database tables...")
    
    # Import all models to register them with Base
    from app.models import bookmark, user
    
    # Create all tables
    BookmarkBase.metadata.create_all(bind=engine)
    UserBase.metadata.create_all(bind=engine)
    
    print("Tables created successfully!")

if __name__ == "__main__":
    create_tables()