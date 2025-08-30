"""Initialize database tables.

Creates all tables defined in models.
"""
from app.core.database import engine
from app.models import Base


def create_tables():
    """Create all database tables."""
    Base.metadata.create_all(bind=engine)
    print("Database tables created successfully!")


if __name__ == "__main__":
    create_tables()