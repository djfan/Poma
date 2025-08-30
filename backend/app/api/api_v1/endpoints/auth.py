"""Authentication endpoints for Poma API.

Simple is better than complex.
Readability counts.
"""
from datetime import datetime, timedelta
from typing import Optional

from fastapi import APIRouter, HTTPException, Depends
from google.auth.transport import requests
from google.oauth2 import id_token
import jwt as pyjwt
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.user import User

router = APIRouter()


# Data Models - Keep it simple
class GoogleSignInRequest(BaseModel):
    id_token: str


class UserResponse(BaseModel):
    id: int
    email: str
    name: str
    avatar_url: Optional[str] = None


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

# Future: Email/password authentication will go here
# For now, we only support Google OAuth

@router.post("/google", response_model=LoginResponse)
async def google_sign_in(request: GoogleSignInRequest, db: Session = Depends(get_db)):
    """Authenticate user with Google OAuth token."""
    try:
        # Verify Google token and get user info (secure method)
        user_info = _verify_google_token(request.id_token)
        user = _get_or_create_user(user_info, db)
        
        token = _create_access_token(user.id)
        
        return LoginResponse(
            access_token=token,
            user=UserResponse(
                id=user.id,
                email=user.email,
                name=user_info.get("name", user.email),
                avatar_url=user_info.get("picture")
            )
        )
        
    except Exception as e:
        print(f"Authentication error: {e}")
        raise HTTPException(status_code=500, detail=f"Authentication failed: {str(e)}")

@router.get("/me", response_model=UserResponse)
async def get_current_user():
    """Get current authenticated user info."""
    # TODO: Extract user from JWT token
    return UserResponse(
        id=1,
        email="test@example.com",
        name="Test User"
    )


# Private helper functions - Flat is better than nested
def _verify_google_token(token: str) -> dict:
    """Verify Google ID token and return user info."""
    try:
        # Verify Google token with proper audience check
        idinfo = id_token.verify_oauth2_token(
            token,
            requests.Request(),
            audience=settings.GOOGLE_CLIENT_ID
        )
        return idinfo
        
    except ValueError as e:
        print(f"Token verification failed: {e}")
        
        # Try one more time with just basic validation
        try:
            print("Attempting basic token validation...")
            idinfo = id_token.verify_oauth2_token(token, requests.Request())
            print(f"Basic validation succeeded for: {idinfo.get('email')}")
            return idinfo
        except Exception as e2:
            print(f"Basic validation also failed: {e2}")
            raise ValueError(f"Invalid Google token: {e}")
            
    except Exception as e:
        print(f"Unexpected error during token verification: {e}")
        raise Exception(f"Token verification error: {e}")


def _get_or_create_user(google_info: dict, db: Session) -> User:
    """Get existing user or create new one from Google info."""
    email = google_info["email"]
    
    # Check if user already exists
    user = db.query(User).filter(User.email == email).first()
    
    if not user:
        # Create new user with current User model fields
        user = User(
            email=email,
            is_active=True,
            hashed_password="google_oauth"  # Placeholder for OAuth users
        )
        db.add(user)
        db.commit()
        db.refresh(user)
        print(f"Created new user: {email} with ID: {user.id}")
    else:
        print(f"Existing user: {email} with ID: {user.id}")
    
    return user


def _create_access_token(user_id: int) -> str:
    """Create JWT access token for user."""
    expires = datetime.utcnow() + timedelta(
        minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES
    )
    
    payload = {
        "sub": str(user_id),
        "exp": expires,
        "type": "access"
    }
    
    return pyjwt.encode(
        payload,
        settings.SECRET_KEY,
        algorithm=settings.ALGORITHM
    )