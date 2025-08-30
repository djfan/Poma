#!/usr/bin/env python3
"""
Production database initialization script.
Creates the PostgreSQL database and tables.
"""
import os
import sys
from sqlalchemy import create_engine
from sqlalchemy.exc import OperationalError

# Add app to Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def create_production_database():
    """Create the production PostgreSQL database and tables."""
    
    # Database connection settings
    DB_HOST = "localhost"
    DB_PORT = "5432"
    DB_NAME = "poma_production"
    DB_USER = "postgres"
    
    # Create database URL
    database_url = f"postgresql://{DB_USER}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
    
    print(f"🔧 Setting up production database: {DB_NAME}")
    
    try:
        # Import models and database setup
        from app.core.database import Base
        from app.models import user, bookmark  # Import to register models
        
        # Create engine
        engine = create_engine(database_url)
        
        print("📋 Creating database tables...")
        Base.metadata.create_all(bind=engine)
        
        print("✅ Production database setup complete!")
        print(f"📊 Database: {database_url}")
        
        # Test connection
        with engine.connect() as conn:
            result = conn.execute("SELECT current_database(), version();")
            db_info = result.fetchone()
            print(f"🗄️  Connected to: {db_info[0]}")
            print(f"🐘 PostgreSQL: {db_info[1].split(',')[0]}")
            
        return True
        
    except OperationalError as e:
        if "does not exist" in str(e):
            print(f"❌ Database '{DB_NAME}' does not exist.")
            print("🔧 Please run: createdb poma_production")
            return False
        else:
            print(f"❌ Database connection error: {e}")
            return False
    except Exception as e:
        print(f"❌ Error setting up database: {e}")
        return False

if __name__ == "__main__":
    success = create_production_database()
    sys.exit(0 if success else 1)