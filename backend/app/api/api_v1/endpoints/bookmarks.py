from fastapi import APIRouter, HTTPException, UploadFile, File, Depends, Header, Form
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from sqlalchemy.orm import Session
import os
import uuid

from app.core.database import get_db
from app.core.auth import get_current_user_id
from app.models.bookmark import Bookmark
from app.models.user import User
from app.services.spotify_service import spotify_service
from app.services.openai_service import openai_service

router = APIRouter()

class BookmarkCreate(BaseModel):
    podcast_name: str
    episode_name: str
    timestamp_ms: int
    spotify_episode_id: Optional[str] = None
    user_note: Optional[str] = None

class BookmarkCreateFromSpotify(BaseModel):
    """Create bookmark from current Spotify playback."""
    user_note: Optional[str] = None

class BookmarkUpdate(BaseModel):
    """Update bookmark transcript text."""
    transcript_text: str

class BookmarkResponse(BaseModel):
    id: int
    podcast_name: str
    episode_name: str
    timestamp_ms: int  # This should handle large values
    duration_ms: Optional[int] = None
    spotify_episode_id: Optional[str] = None
    podcast_cover_url: Optional[str] = None
    audio_file_path: Optional[str] = None
    transcript_text: Optional[str] = None
    user_note: Optional[str] = None
    ai_summary: Optional[str] = None
    created_at: datetime
    # MediaSession fields for deep linking
    media_id: Optional[str] = None
    source_app_package: Optional[str] = None
    album_art_uri: Optional[str] = None
    
    class Config:
        orm_mode = True

@router.post("/", response_model=BookmarkResponse)
async def create_bookmark(
    bookmark: BookmarkCreate, 
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Create a new bookmark."""
    db_bookmark = Bookmark(
        user_id=current_user_id,
        podcast_name=bookmark.podcast_name,
        episode_name=bookmark.episode_name,
        spotify_episode_id=bookmark.spotify_episode_id,
        timestamp_ms=bookmark.timestamp_ms,
        user_note=bookmark.user_note
    )
    
    db.add(db_bookmark)
    db.commit()
    db.refresh(db_bookmark)
    
    return db_bookmark

@router.post("/{bookmark_id}/audio")
async def upload_audio(bookmark_id: int, audio: UploadFile = File(...)):
    # TODO: 实现音频文件上传和转录
    return {"message": f"Audio uploaded for bookmark {bookmark_id}"}

@router.get("/current-playback")
async def get_current_playback():
    """Get current Spotify playback status (shows any playing music/podcast)."""
    try:
        # Try to get current playback from Spotify using client credentials
        # This is a simple implementation that doesn't require user auth
        import spotipy
        from spotipy.oauth2 import SpotifyClientCredentials
        
        try:
            client_credentials_manager = SpotifyClientCredentials(
                client_id=spotify_service.client_id,
                client_secret=spotify_service.client_secret
            )
            sp = spotipy.Spotify(client_credentials_manager=client_credentials_manager)
            
            # Note: Client credentials flow doesn't allow access to user's current playback
            # We would need user authorization for that. For now, return mock data indicating
            # Spotify is available but no playback info without user auth.
            return {
                "is_playing": False,
                "track_name": "Connect Spotify for live playback",
                "artist_name": "Authentication required",
                "album_name": "",
                "spotify_available": True  # Spotify service is available
            }
        except Exception as spotify_error:
            print(f"Spotify client credentials error: {spotify_error}")
            return {
                "is_playing": False,
                "track_name": "No music playing",
                "artist_name": "",
                "album_name": "",
                "spotify_available": False
            }
    except Exception as e:
        return {
            "is_playing": False,
            "track_name": "No music playing", 
            "artist_name": "",
            "album_name": "",
            "spotify_available": False
        }

@router.get("/debug")
async def debug_bookmarks(
    db: Session = Depends(get_db)
):
    """Debug endpoint to return raw bookmark data."""
    bookmarks = db.query(Bookmark).filter(
        Bookmark.user_id == 2
    ).limit(1).all()
    
    # Convert to dict manually to see what we have
    result = []
    for bookmark in bookmarks:
        data = {}
        for column in bookmark.__table__.columns:
            value = getattr(bookmark, column.name)
            if value is None:
                data[column.name] = None
            else:
                data[column.name] = str(value)
        result.append(data)
    
    return {"debug": True, "count": len(result), "data": result}

class SimpleBookmarkTest(BaseModel):
    id: int
    podcast_name: str
    episode_name: str
    timestamp_ms: int
    created_at: datetime
    
    class Config:
        orm_mode = True

@router.get("/test-simple", response_model=SimpleBookmarkTest)
async def test_simple_bookmark(
    db: Session = Depends(get_db)
):
    """Test pydantic conversion with minimal fields."""
    bookmark = db.query(Bookmark).filter(
        Bookmark.user_id == 2
    ).first()
    if not bookmark:
        raise HTTPException(status_code=404, detail="No bookmarks found")
    return bookmark

@router.get("/")
async def get_bookmarks(
    skip: int = 0, 
    limit: int = 100, 
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Get list of bookmarks for current user."""
    bookmarks = db.query(Bookmark).filter(
        Bookmark.user_id == current_user_id
    ).offset(skip).limit(limit).all()
    
    # Manually convert to dict to avoid pydantic validation issues
    result = []
    for bookmark in bookmarks:
        result.append({
            "id": bookmark.id,
            "podcast_name": bookmark.podcast_name,
            "episode_name": bookmark.episode_name,
            "timestamp_ms": bookmark.timestamp_ms,
            "duration_ms": bookmark.duration_ms,
            "spotify_episode_id": bookmark.spotify_episode_id,
            "podcast_cover_url": bookmark.podcast_cover_url,
            "audio_file_path": bookmark.audio_file_path,
            "transcript_text": bookmark.transcript_text,
            "user_note": bookmark.user_note,
            "ai_summary": bookmark.ai_summary,
            "created_at": bookmark.created_at.isoformat() if bookmark.created_at else None,
            "media_id": bookmark.media_id,
            "source_app_package": bookmark.source_app_package,
            "album_art_uri": bookmark.album_art_uri
        })
    
    return result

@router.get("/{bookmark_id}", response_model=BookmarkResponse)
async def get_bookmark(bookmark_id: int, db: Session = Depends(get_db)):
    """Get a specific bookmark by ID."""
    bookmark = db.query(Bookmark).filter(Bookmark.id == bookmark_id).first()
    if not bookmark:
        raise HTTPException(status_code=404, detail="Bookmark not found")
    return bookmark

@router.delete("/{bookmark_id}")
async def delete_bookmark(
    bookmark_id: int, 
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Delete a bookmark."""
    bookmark = db.query(Bookmark).filter(
        Bookmark.id == bookmark_id,
        Bookmark.user_id == current_user_id
    ).first()
    if not bookmark:
        raise HTTPException(status_code=404, detail="Bookmark not found")
    
    db.delete(bookmark)
    db.commit()
    return {"message": f"Bookmark {bookmark_id} deleted successfully"}

@router.put("/{bookmark_id}", response_model=BookmarkResponse)
async def update_bookmark(
    bookmark_id: int,
    bookmark_update: BookmarkUpdate,
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Update a bookmark's transcript text."""
    bookmark = db.query(Bookmark).filter(
        Bookmark.id == bookmark_id,
        Bookmark.user_id == current_user_id
    ).first()
    if not bookmark:
        raise HTTPException(status_code=404, detail="Bookmark not found")
    
    # Update the transcript text
    bookmark.transcript_text = bookmark_update.transcript_text
    
    db.commit()
    db.refresh(bookmark)
    return bookmark

@router.post("/from-spotify", response_model=BookmarkResponse)
async def create_bookmark_from_spotify(
    bookmark_data: BookmarkCreateFromSpotify,
    authorization: str = Header(..., description="Bearer token from Spotify OAuth"),
    db: Session = Depends(get_db)
):
    """Create a bookmark from current Spotify playback."""
    # Extract access token from Authorization header
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=400, detail="Invalid authorization header format")
    
    access_token = authorization.replace("Bearer ", "")
    
    # Get current playback from Spotify
    playback = spotify_service.get_current_playback(access_token)
    if not playback:
        raise HTTPException(
            status_code=400, 
            detail="No podcast currently playing on Spotify or playback not available"
        )
    
    # Create bookmark with Spotify data
    db_bookmark = Bookmark(
        user_id=1,  # TODO: Get from JWT token
        podcast_name=playback["podcast_name"],
        episode_name=playback["episode_name"],
        spotify_episode_id=playback["episode_id"],
        timestamp_ms=playback["progress_ms"],
        duration_ms=playback["duration_ms"],
        user_note=bookmark_data.user_note,
        podcast_cover_url=playback["images"][0]["url"] if playback["images"] else None
    )
    
    db.add(db_bookmark)
    db.commit()
    db.refresh(db_bookmark)
    
    return db_bookmark

@router.post("/voice-bookmark", response_model=BookmarkResponse)
async def create_voice_bookmark(
    audio: UploadFile = File(...),
    # MediaSession data from Android MediaSessionManager
    media_title: str = Form(None),
    media_artist: str = Form(None), 
    media_id: str = Form(None),
    source_app_package: str = Form(None),
    album_art_uri: str = Form(None),
    timestamp_ms: int = Form(None),
    duration_ms: int = Form(None),
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Create a bookmark with voice note using MediaSession data from Android."""
    # Validate audio file
    if not audio.content_type or not audio.content_type.startswith("audio/"):
        raise HTTPException(status_code=400, detail="File must be an audio file")
    
    # Check if we have MediaSession data
    media_session_available = bool(media_title and media_artist and timestamp_ms)
    
    if media_session_available:
        # Log MediaSession data for debugging
        progress_min = timestamp_ms // 60000
        progress_sec = (timestamp_ms % 60000) // 1000
        print(f"DEBUG: MediaSession data received:")
        print(f"  App: {source_app_package}")
        print(f"  Title: {media_title}")
        print(f"  Artist: {media_artist}")
        print(f"  Timestamp: {progress_min}:{progress_sec:02d} ({timestamp_ms}ms)")
        print(f"  MediaID: {media_id}")
        print(f"  Album Art: {album_art_uri}")
    else:
        print("DEBUG: No MediaSession data available, creating generic bookmark")
    
    # Create uploads directory if it doesn't exist
    upload_dir = "uploads/audio"
    os.makedirs(upload_dir, exist_ok=True)
    
    # Generate unique filename
    file_extension = os.path.splitext(audio.filename)[1] if audio.filename else ".m4a"
    unique_filename = f"{uuid.uuid4()}{file_extension}"
    file_path = os.path.join(upload_dir, unique_filename)
    
    # Save audio file temporarily for transcription
    try:
        with open(file_path, "wb") as buffer:
            content = await audio.read()
            buffer.write(content)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to save audio file: {str(e)}")
    
    # Transcribe audio to text using OpenAI Whisper
    transcript_text = await openai_service.transcribe_audio(file_path)
    
    # Delete the audio file after transcription (save storage)
    try:
        os.remove(file_path)
        print(f"Audio file deleted: {file_path}")
    except Exception as e:
        print(f"Warning: Could not delete audio file {file_path}: {e}")
    
    # Create bookmark with MediaSession data or fallback to generic recording
    if media_session_available:
        # Create bookmark with MediaSession data from Android
        db_bookmark = Bookmark(
            user_id=current_user_id,
            podcast_name=media_artist,  # Artist field usually contains podcast name
            episode_name=media_title,  # Title field contains episode name
            spotify_episode_id=None,   # Legacy field, not used with MediaSession
            timestamp_ms=timestamp_ms,
            duration_ms=duration_ms,
            transcript_text=transcript_text,
            audio_file_path=None,      # No longer store audio files
            podcast_cover_url=None,    # Legacy field, use album_art_uri instead
            # New MediaSession fields
            media_id=media_id,
            source_app_package=source_app_package,
            album_art_uri=album_art_uri
        )
    else:
        # Create regular voice recording without media data
        current_time = int(datetime.now().timestamp() * 1000)  # Current timestamp in ms
        db_bookmark = Bookmark(
            user_id=current_user_id,
            podcast_name="Voice Recording",
            episode_name=f"Recording {datetime.now().strftime('%Y-%m-%d %H:%M')}",
            spotify_episode_id=None,
            timestamp_ms=current_time,
            duration_ms=None,
            transcript_text=transcript_text,
            audio_file_path=None,
            podcast_cover_url=None,
            # New MediaSession fields
            media_id=None,
            source_app_package=None,
            album_art_uri=None
        )
    
    try:
        print(f"DEBUG: Adding bookmark to database with timestamp {db_bookmark.timestamp_ms}ms")
        db.add(db_bookmark)
        db.commit()
        db.refresh(db_bookmark)
        print(f"DEBUG: Bookmark successfully created with ID: {db_bookmark.id}")
        return db_bookmark
    except Exception as db_error:
        print(f"ERROR: Database commit failed: {db_error}")
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {str(db_error)}")
