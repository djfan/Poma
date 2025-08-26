from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from google.oauth2 import id_token
from google.auth.transport import requests
from app.core.config import settings
import jwt as pyjwt
from datetime import datetime, timedelta

router = APIRouter()

class UserCreate(BaseModel):
    email: str
    password: str

class UserLogin(BaseModel):
    email: str
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str

class GoogleSignInRequest(BaseModel):
    id_token: str

class UserResponse(BaseModel):
    id: int
    email: str
    name: str
    avatar_url: str = None

class LoginResponse(BaseModel):
    access_token: str
    token_type: str
    user: UserResponse

@router.post("/register", response_model=Token)
async def register(user: UserCreate):
    # TODO: 实现用户注册逻辑
    return {"access_token": "fake_token", "token_type": "bearer"}

@router.post("/login", response_model=Token)
async def login(user_credentials: UserLogin):
    # TODO: 实现用户登录逻辑
    return {"access_token": "fake_token", "token_type": "bearer"}

@router.post("/google", response_model=LoginResponse)
async def google_sign_in(request: GoogleSignInRequest):
    """Google OAuth 登录"""
    try:
        # 验证 Google ID Token
        idinfo = id_token.verify_oauth2_token(
            request.id_token, 
            requests.Request(), 
            settings.GOOGLE_CLIENT_ID
        )

        # 提取用户信息
        email = idinfo['email']
        name = idinfo.get('name', '')
        google_id = idinfo['sub']
        avatar_url = idinfo.get('picture')

        # TODO: 从数据库获取或创建用户
        # 暂时使用模拟数据
        user_data = {
            "id": 1,
            "email": email,
            "name": name,
            "avatar_url": avatar_url
        }
        
        # 生成 JWT Token
        access_token = create_access_token(user_id=user_data["id"])
        
        return LoginResponse(
            access_token=access_token,
            token_type="bearer",
            user=UserResponse(**user_data)
        )
        
    except ValueError as e:
        raise HTTPException(status_code=400, detail="Invalid Google token")
    except Exception as e:
        raise HTTPException(status_code=500, detail="Internal server error")

@router.get("/me", response_model=UserResponse)
async def get_current_user():
    # TODO: 实现获取当前用户信息
    return UserResponse(
        id=1, 
        email="test@example.com",
        name="Test User"
    )

def create_access_token(user_id: int) -> str:
    """生成 JWT Token"""
    expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode = {
        "sub": str(user_id),
        "exp": expire,
        "type": "access"
    }
    encoded_jwt = pyjwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt