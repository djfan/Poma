"""Poma API - Simple podcast bookmarking service.

Beautiful is better than ugly.
Simple is better than complex.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.api_v1.api import api_router
from app.core.config import settings


def create_app() -> FastAPI:
    """Create and configure FastAPI application."""
    app = FastAPI(
        title="Poma API",
        description="Simple podcast bookmarking service",
        version="1.0.0",
        debug=settings.DEBUG
    )
    
    _configure_cors(app)
    _configure_routes(app)
    
    return app


def _configure_cors(app: FastAPI) -> None:
    """Configure CORS middleware for development."""
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"] if settings.DEBUG else ["http://localhost"],
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "DELETE"],
        allow_headers=["*"],
    )


def _configure_routes(app: FastAPI) -> None:
    """Configure all application routes."""
    app.include_router(api_router, prefix=settings.API_V1_STR)
    
    @app.get("/")
    async def root():
        return {"message": "Poma API"}
    
    @app.get("/health")
    async def health():
        return {"status": "ok"}
    
    @app.get("/debug/config")
    async def debug_config():
        """Debug endpoint to check configuration."""
        import os
        return {
            "database_url_env": os.environ.get("DATABASE_URL", "NOT_SET"),
            "database_url_settings": settings.DATABASE_URL[:50] + "..." if len(settings.DATABASE_URL) > 50 else settings.DATABASE_URL,
            "debug_mode": settings.DEBUG,
            "google_client_id": settings.GOOGLE_CLIENT_ID[:20] + "..." if settings.GOOGLE_CLIENT_ID else None
        }
    
    @app.post("/debug/create-tables")
    async def create_tables():
        """Debug endpoint to create database tables."""
        from sqlalchemy import create_engine, text
        from app.core.database import Base
        from app.models.user import User
        from app.models.bookmark import Bookmark
        import os
        
        try:
            # Get database URL
            database_url = os.environ.get("DATABASE_URL", settings.DATABASE_URL)
            
            # Create engine
            engine = create_engine(database_url, echo=False)
            
            # Test connection and get version
            with engine.connect() as conn:
                result = conn.execute(text("SELECT version()"))
                db_version = result.fetchone()[0]
            
            # Create all tables
            Base.metadata.create_all(bind=engine)
            
            # Verify tables were created
            with engine.connect() as conn:
                result = conn.execute(text("""
                    SELECT table_name 
                    FROM information_schema.tables 
                    WHERE table_schema = 'public'
                    ORDER BY table_name
                """))
                tables = [table[0] for table in result.fetchall()]
            
            return {
                "success": True,
                "message": "Database tables created successfully",
                "database_version": db_version[:50],
                "tables_created": tables
            }
            
        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "message": "Failed to create database tables"
            }


app = create_app()