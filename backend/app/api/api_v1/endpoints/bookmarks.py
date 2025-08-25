from fastapi import APIRouter, HTTPException, UploadFile, File
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

router = APIRouter()

class BookmarkCreate(BaseModel):
    podcast_name: str
    episode_name: str
    timestamp_ms: int
    spotify_episode_id: Optional[str] = None
    user_note: Optional[str] = None

class BookmarkResponse(BaseModel):
    id: int
    podcast_name: str
    episode_name: str
    timestamp_ms: int
    transcript_text: Optional[str] = None
    user_note: Optional[str] = None
    ai_summary: Optional[str] = None
    created_at: datetime
    
    class Config:
        from_attributes = True

@router.post("/", response_model=BookmarkResponse)
async def create_bookmark(bookmark: BookmarkCreate):
    # TODO: 实现创建书签逻辑
    return BookmarkResponse(
        id=1,
        podcast_name=bookmark.podcast_name,
        episode_name=bookmark.episode_name,
        timestamp_ms=bookmark.timestamp_ms,
        transcript_text="Sample transcript",
        user_note=bookmark.user_note,
        created_at=datetime.now()
    )

@router.post("/{bookmark_id}/audio")
async def upload_audio(bookmark_id: int, audio: UploadFile = File(...)):
    # TODO: 实现音频文件上传和转录
    return {"message": f"Audio uploaded for bookmark {bookmark_id}"}

@router.get("/", response_model=List[BookmarkResponse])
async def get_bookmarks(skip: int = 0, limit: int = 100):
    # TODO: 实现获取书签列表
    return []

@router.get("/{bookmark_id}", response_model=BookmarkResponse)
async def get_bookmark(bookmark_id: int):
    # TODO: 实现获取单个书签
    raise HTTPException(status_code=404, detail="Bookmark not found")

@router.delete("/{bookmark_id}")
async def delete_bookmark(bookmark_id: int):
    # TODO: 实现删除书签
    return {"message": f"Bookmark {bookmark_id} deleted"}