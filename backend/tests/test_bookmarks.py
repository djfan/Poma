"""Tests for bookmark API endpoints."""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.main import app
from app.core.database import get_db, Base
from app.models.user import User

# Create test database
SQLALCHEMY_DATABASE_URL = "postgresql:///poma_test"
engine = create_engine(SQLALCHEMY_DATABASE_URL)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def override_get_db():
    """Override database dependency for testing."""
    try:
        db = TestingSessionLocal()
        yield db
    finally:
        db.close()


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture
def client():
    """Create test client."""
    # Create test tables
    Base.metadata.create_all(bind=engine)
    
    # Create test user
    db = TestingSessionLocal()
    test_user = User(email="test@example.com", hashed_password="dummy_hash")
    db.add(test_user)
    db.commit()
    db.close()
    
    with TestClient(app) as c:
        yield c
    
    # Clean up
    Base.metadata.drop_all(bind=engine)


def test_create_bookmark(client):
    """Test bookmark creation."""
    response = client.post(
        "/api/v1/bookmarks/",
        json={
            "podcast_name": "Test Podcast",
            "episode_name": "Test Episode",
            "timestamp_ms": 120000,
            "user_note": "Test note"
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert data["podcast_name"] == "Test Podcast"
    assert data["episode_name"] == "Test Episode"
    assert data["timestamp_ms"] == 120000
    assert data["user_note"] == "Test note"
    assert "id" in data
    assert "created_at" in data


def test_get_bookmarks(client):
    """Test getting bookmarks list."""
    # Create a bookmark first
    client.post(
        "/api/v1/bookmarks/",
        json={
            "podcast_name": "Test Podcast",
            "episode_name": "Test Episode", 
            "timestamp_ms": 120000
        }
    )
    
    # Get bookmarks
    response = client.get("/api/v1/bookmarks/")
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) == 1
    assert data[0]["podcast_name"] == "Test Podcast"


def test_get_bookmark_by_id(client):
    """Test getting specific bookmark."""
    # Create a bookmark
    create_response = client.post(
        "/api/v1/bookmarks/",
        json={
            "podcast_name": "Test Podcast",
            "episode_name": "Test Episode",
            "timestamp_ms": 120000
        }
    )
    bookmark_id = create_response.json()["id"]
    
    # Get specific bookmark
    response = client.get(f"/api/v1/bookmarks/{bookmark_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == bookmark_id
    assert data["podcast_name"] == "Test Podcast"


def test_delete_bookmark(client):
    """Test bookmark deletion."""
    # Create a bookmark
    create_response = client.post(
        "/api/v1/bookmarks/",
        json={
            "podcast_name": "Test Podcast",
            "episode_name": "Test Episode",
            "timestamp_ms": 120000
        }
    )
    bookmark_id = create_response.json()["id"]
    
    # Delete bookmark
    response = client.delete(f"/api/v1/bookmarks/{bookmark_id}")
    assert response.status_code == 200
    assert "deleted successfully" in response.json()["message"]
    
    # Verify it's gone
    get_response = client.get(f"/api/v1/bookmarks/{bookmark_id}")
    assert get_response.status_code == 404