from fastapi import APIRouter
from app.api.api_v1.endpoints import auth, bookmarks, spotify

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["auth"])
api_router.include_router(bookmarks.router, prefix="/bookmarks", tags=["bookmarks"])
api_router.include_router(spotify.router, prefix="/spotify", tags=["spotify"])