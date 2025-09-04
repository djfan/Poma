#!/usr/bin/env python3
"""
Create database tables for Poma cloud production environment.

This script creates all necessary tables in the cloud PostgreSQL database.
"""
import os
import sys
from sqlalchemy import create_engine, text

# Add the backend directory to the Python path
sys.path.insert(0, os.path.dirname(__file__))

from app.core.database import Base
from app.models.user import User
from app.models.bookmark import Bookmark
from app.core.config import settings

def create_tables():
    """Create all tables in the cloud database."""
    
    # Use the DATABASE_URL from environment or config
    database_url = os.environ.get("DATABASE_URL", settings.DATABASE_URL)
    
    print(f"Creating tables in database: {database_url[:50]}...")
    
    try:
        # Create engine
        engine = create_engine(database_url, echo=True)
        
        # Test connection
        with engine.connect() as conn:
            result = conn.execute(text("SELECT version()"))
            print(f"Connected to PostgreSQL: {result.fetchone()[0]}")
        
        # Create all tables
        print("Creating all tables...")
        Base.metadata.create_all(bind=engine)
        
        # Verify tables were created
        with engine.connect() as conn:
            result = conn.execute(text("""
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'public'
                ORDER BY table_name
            """))
            tables = result.fetchall()
            print("\nTables created:")
            for table in tables:
                print(f"  - {table[0]}")
        
        print("\n✅ Database tables created successfully!")
        
    except Exception as e:
        print(f"❌ Error creating tables: {e}")
        return False
    
    return True

if __name__ == "__main__":
    success = create_tables()
    sys.exit(0 if success else 1)