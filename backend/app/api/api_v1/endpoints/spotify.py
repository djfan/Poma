from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

class SpotifyAuthUrl(BaseModel):
    auth_url: str

class CurrentPlayback(BaseModel):
    is_playing: bool
    track_name: Optional[str] = None
    artist_name: Optional[str] = None
    progress_ms: Optional[int] = None
    duration_ms: Optional[int] = None
    episode_id: Optional[str] = None
    podcast_name: Optional[str] = None

@router.get("/auth-url", response_model=SpotifyAuthUrl)
async def get_spotify_auth_url():
    # TODO: 实现 Spotify OAuth 授权 URL 生成
    auth_url = "https://accounts.spotify.com/authorize?client_id=your_client_id&response_type=code&redirect_uri=your_redirect_uri&scope=user-read-playback-state"
    return {"auth_url": auth_url}

@router.post("/callback")
async def spotify_callback(code: str):
    # TODO: 实现 Spotify OAuth 回调处理
    return {"message": "Spotify authorization successful"}

@router.get("/current-playback", response_model=CurrentPlayback)
async def get_current_playback():
    # TODO: 实现获取当前播放状态
    return {
        "is_playing": True,
        "track_name": "Example Podcast Episode",
        "artist_name": "Example Podcast",
        "progress_ms": 120000,
        "duration_ms": 3600000,
        "episode_id": "spotify:episode:example123",
        "podcast_name": "Example Podcast"
    }