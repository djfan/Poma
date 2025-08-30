#!/usr/bin/env python3
"""Initialize production database with proper environment setup."""
import os
import sys
from pathlib import Path

# Set production environment before any imports
os.environ['DATABASE_URL'] = 'postgresql://djfan@localhost:5432/poma_production'
os.environ['DEBUG'] = 'False'

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent))

def init_production_database():
    """Initialize the production database."""
    try:
        # Import after setting environment
        from sqlalchemy import create_engine, text
        from app.models.user import User
        from app.models.bookmark import Bookmark
        from app.core.database import Base
        
        # Create engine with production URL
        database_url = os.environ['DATABASE_URL']
        engine = create_engine(database_url)
        
        print(f"🔧 Initializing production database...")
        print(f"📍 Database URL: {database_url}")
        
        # Create all tables
        print("📋 Creating database schema...")
        Base.metadata.create_all(bind=engine)
        
        # Verify tables were created
        print("🔍 Verifying table creation...")
        with engine.connect() as conn:
            result = conn.execute(text("SELECT tablename FROM pg_tables WHERE schemaname = 'public'"))
            tables = [row[0] for row in result.fetchall()]
            
        if tables:
            print(f"✅ Successfully created tables: {', '.join(tables)}")
            
            # Show table details
            for table in tables:
                with engine.connect() as conn:
                    result = conn.execute(text(f"SELECT count(*) FROM information_schema.columns WHERE table_name = '{table}'"))
                    column_count = result.scalar()
                    print(f"   📊 {table}: {column_count} columns")
                    
            return True
        else:
            print("❌ No tables found in database")
            return False
            
    except Exception as e:
        print(f"❌ Error initializing database: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = init_production_database()
    if success:
        print("\n🎉 Production database initialization complete!")
        print("🚀 Ready for production deployment")
    else:
        print("\n💥 Database initialization failed")
    
    sys.exit(0 if success else 1)