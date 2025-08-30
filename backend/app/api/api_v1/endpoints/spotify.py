"""Spotify API endpoints for Poma.

Simple is better than complex.
Readability counts.
"""
from fastapi import APIRouter, HTTPException, Header, Depends, Query
from fastapi.responses import RedirectResponse
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from sqlalchemy.orm import Session
import logging

from app.core.database import get_db
from app.core.auth import get_current_user_id
from app.services.spotify_service import spotify_service
from app.models.user import User

logger = logging.getLogger(__name__)
router = APIRouter()


# Response Models
class SpotifyAuthUrl(BaseModel):
    auth_url: str


class SpotifyCallbackRequest(BaseModel):
    code: str

class SpotifyCallback(BaseModel):
    access_token: str
    message: str


class PodcastPlayback(BaseModel):
    is_playing: bool
    progress_ms: int
    episode_id: str
    episode_name: str
    duration_ms: int
    podcast_name: str
    podcast_publisher: Optional[str] = None
    episode_uri: str
    release_date: Optional[str] = None


class EpisodeDetails(BaseModel):
    id: str
    name: str
    description: str
    duration_ms: int
    podcast_name: str
    podcast_publisher: Optional[str] = None
    release_date: Optional[str] = None


@router.get("/auth-url", response_model=SpotifyAuthUrl)
async def get_spotify_auth_url():
    """Get Spotify OAuth authorization URL."""
    try:
        auth_url = spotify_service.get_authorization_url()
        return SpotifyAuthUrl(auth_url=auth_url)
    except Exception as e:
        logger.error(f"Error generating Spotify auth URL: {e}")
        raise HTTPException(status_code=500, detail="Failed to generate auth URL")


@router.post("/callback", response_model=SpotifyCallback)
async def spotify_callback(
    request: SpotifyCallbackRequest,
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Handle Spotify OAuth callback and store token."""
    try:
        access_token = spotify_service.get_access_token(request.code)
        if not access_token:
            raise HTTPException(status_code=400, detail="Invalid authorization code")
        
        # Store token in user record
        user = db.query(User).filter(User.id == current_user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        
        user.spotify_access_token = access_token
        db.commit()
        
        return SpotifyCallback(
            access_token=access_token,
            message="Spotify authorization successful"
        )
    except Exception as e:
        logger.error(f"Error in Spotify callback: {e}")
        raise HTTPException(status_code=500, detail="Authorization failed")


@router.get("/current-playback", response_model=Optional[PodcastPlayback])
async def get_current_playback(
    db: Session = Depends(get_db),
    current_user_id: int = Depends(get_current_user_id)
):
    """Get current podcast playback status."""
    try:
        # Get user's stored Spotify token
        user = db.query(User).filter(User.id == current_user_id).first()
        if not user or not user.spotify_access_token:
            return None  # No Spotify token available
        
        playback = spotify_service.get_current_playback(user.spotify_access_token)
        
        if not playback:
            return None
        
        return PodcastPlayback(
            is_playing=playback['is_playing'],
            progress_ms=playback['progress_ms'],
            episode_id=playback['episode_id'],
            episode_name=playback['episode_name'],
            duration_ms=playback['duration_ms'],
            podcast_name=playback['podcast_name'],
            podcast_publisher=playback.get('podcast_publisher'),
            episode_uri=playback['episode_uri'],
            release_date=playback.get('release_date')
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting current playback: {e}")
        raise HTTPException(status_code=500, detail="Failed to get playback status")


@router.get("/episode/{episode_id}", response_model=EpisodeDetails)
async def get_episode_details(episode_id: str, authorization: str = Header(...)):
    """Get detailed episode information."""
    try:
        if not authorization.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Invalid authorization header")
        
        access_token = authorization.split(" ")[1]
        episode = spotify_service.get_episode_details(access_token, episode_id)
        
        if not episode:
            raise HTTPException(status_code=404, detail="Episode not found")
        
        return EpisodeDetails(
            id=episode['id'],
            name=episode['name'],
            description=episode['description'],
            duration_ms=episode['duration_ms'],
            podcast_name=episode['podcast']['name'],
            podcast_publisher=episode['podcast'].get('publisher'),
            release_date=episode.get('release_date')
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting episode details: {e}")
        raise HTTPException(status_code=500, detail="Failed to get episode details")


@router.get("/callback-web")
async def spotify_callback_web(
    code: Optional[str] = Query(None),
    error: Optional[str] = Query(None),
    state: Optional[str] = Query(None)
):
    """Handle Spotify OAuth web callback and redirect to app."""
    if error:
        logger.error(f"Spotify OAuth error: {error}")
        return RedirectResponse(url=f"poma://spotify/error?error={error}")
    
    if not code:
        logger.error("No authorization code received")
        return RedirectResponse(url="poma://spotify/error?error=no_code")
    
    # Redirect to app with the authorization code
    return RedirectResponse(url=f"poma://spotify?code={code}")