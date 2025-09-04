# Poma Backend API Endpoints Documentation

## Base URLs
- **Local Development**: `http://localhost:8001/`
- **Cloud Production**: `https://poma-2sxi.onrender.com/`

## API v1 Base Path
All endpoints are prefixed with `/api/v1/`

---

## Authentication Endpoints
**Base Path**: `/api/v1/auth/`

### POST /api/v1/auth/google
**Description**: Authenticate user with Google OAuth ID token

**Request Body**:
```json
{
    "id_token": "string"
}
```

**Response** (200):
```json
{
    "access_token": "string",
    "token_type": "bearer",
    "user": {
        "id": 1,
        "email": "user@example.com",
        "name": "User Name",
        "avatar_url": "https://..."
    }
}
```

**Errors**: 500 if authentication fails

### GET /api/v1/auth/me
**Description**: Get current authenticated user info

**Headers**: 
- `Authorization: Bearer <token>`

**Response** (200):
```json
{
    "id": 1,
    "email": "user@example.com", 
    "name": "User Name",
    "avatar_url": "https://..."
}
```

---

## Spotify Integration Endpoints
**Base Path**: `/api/v1/spotify/`

### GET /api/v1/spotify/auth-url
**Description**: Get Spotify OAuth authorization URL

**Response** (200):
```json
{
    "auth_url": "https://accounts.spotify.com/authorize?..."
}
```

### POST /api/v1/spotify/callback
**Description**: Handle Spotify OAuth callback with authorization code

**Request Body**:
```json
{
    "code": "string"
}
```

**Response** (200):
```json
{
    "access_token": "string",
    "refresh_token": "string",
    "expires_in": 3600
}
```

---

## Voice Recording Endpoints
**Base Path**: `/api/v1/voice/`

### POST /api/v1/voice/transcribe
**Description**: Transcribe audio file to text

**Request**: Multipart form data
- `audio_file`: Audio file (required)

**Response** (200):
```json
{
    "transcript": "transcribed text here",
    "duration": 45.2
}
```

---

## Playback Endpoints  
**Base Path**: `/api/v1/playback/`

### GET /api/v1/playback/current
**Description**: Get current playback state

**Response** (200):
```json
{
    "is_playing": true,
    "track_name": "Episode Title",
    "show_name": "Podcast Name",
    "progress_ms": 120000,
    "duration_ms": 1800000
}
```

---

## Bookmarks Endpoints
**Base Path**: `/api/v1/bookmarks/`

### GET /api/v1/bookmarks/
**Description**: Get all bookmarks for authenticated user

**Headers**: 
- `Authorization: Bearer <token>`

**Response** (200):
```json
{
    "bookmarks": [
        {
            "id": 1,
            "title": "Bookmark Title",
            "transcript": "bookmark content",
            "timestamp": "2024-01-01T12:00:00Z",
            "episode_name": "Episode Title",
            "show_name": "Podcast Name"
        }
    ]
}
```

### POST /api/v1/bookmarks/
**Description**: Create new bookmark

**Headers**: 
- `Authorization: Bearer <token>`

**Request Body**:
```json
{
    "title": "Bookmark Title",
    "transcript": "bookmark content", 
    "episode_name": "Episode Title",
    "show_name": "Podcast Name"
}
```

**Response** (201):
```json
{
    "id": 1,
    "title": "Bookmark Title",
    "transcript": "bookmark content",
    "timestamp": "2024-01-01T12:00:00Z",
    "episode_name": "Episode Title", 
    "show_name": "Podcast Name"
}
```

### DELETE /api/v1/bookmarks/{bookmark_id}
**Description**: Delete a bookmark

**Headers**: 
- `Authorization: Bearer <token>`

**Response** (204): No content

---

## Environment Configuration

### Local Development
- Backend runs on `http://localhost:8001`
- Uses local PostgreSQL database
- Google OAuth client ID: `882585452174-e4ehsoof2jm9ccs3olh16t5gk9mtokkq.apps.googleusercontent.com`

### Cloud Production  
- Backend runs on `https://poma-2sxi.onrender.com`
- Uses cloud PostgreSQL database
- Same Google OAuth client ID (configured for both domains)

---

## Error Responses

All endpoints may return these common error responses:

**400 Bad Request**:
```json
{
    "detail": "Invalid request data"
}
```

**401 Unauthorized**:
```json
{
    "detail": "Not authenticated"
}
```

**404 Not Found**:
```json
{
    "detail": "Resource not found"
}
```

**500 Internal Server Error**:
```json
{
    "detail": "Internal server error message"
}
```

---

## Notes
- All timestamps are in ISO 8601 format (UTC)
- File uploads use multipart/form-data
- Authentication uses JWT Bearer tokens
- API supports both local development and cloud production environments
- Environment switching handled by ApiConfig in Android app