"""Spotify API integration service.

Simple is better than complex.
Explicit is better than implicit.
"""
import spotipy
from spotipy.oauth2 import SpotifyOAuth
from typing import Optional, Dict, Any
import logging

from app.core.config import settings

logger = logging.getLogger(__name__)


class SpotifyService:
    """Spotify API service for getting playback status and podcast information."""
    
    def __init__(self):
        self.client_id = settings.SPOTIFY_CLIENT_ID or "demo_client_id"
        self.client_secret = settings.SPOTIFY_CLIENT_SECRET or "demo_client_secret"
        self.redirect_uri = "https://developer.spotify.com/dashboard/"
        self.scope = "user-read-playback-state user-read-currently-playing user-read-recently-played"
    
    def get_auth_manager(self) -> SpotifyOAuth:
        """Get Spotify OAuth manager."""
        return SpotifyOAuth(
            client_id=self.client_id,
            client_secret=self.client_secret,
            redirect_uri=self.redirect_uri,
            scope=self.scope
        )
    
    def get_authorization_url(self) -> str:
        """Get Spotify authorization URL for user login."""
        auth_manager = self.get_auth_manager()
        return auth_manager.get_authorize_url()
    
    def get_access_token(self, code: str) -> Optional[str]:
        """Exchange authorization code for access token."""
        try:
            auth_manager = self.get_auth_manager()
            token_info = auth_manager.get_access_token(code)
            return token_info.get('access_token') if token_info else None
        except Exception as e:
            logger.error(f"Error getting Spotify access token: {e}")
            return None
    
    def get_current_playback(self, access_token: str) -> Optional[Dict[str, Any]]:
        """Get current playback status from Spotify."""
        try:
            sp = spotipy.Spotify(auth=access_token)
            current = sp.current_playback()
            
            # Debug logging
            print(f"DEBUG: Spotify current playback: {current is not None}")
            if current:
                print(f"DEBUG: Is playing: {current.get('is_playing')}")
                print(f"DEBUG: Current keys: {list(current.keys())}")
                item = current.get('item')
                if item:
                    print(f"DEBUG: Item type: {item.get('type')}")
                    print(f"DEBUG: Item name: {item.get('name')}")
                else:
                    print("DEBUG: No item in current playbook - trying alternative methods...")
                    print(f"DEBUG: Currently playing type: {current.get('currently_playing_type')}")
                    print(f"DEBUG: Is private session: {current.get('device', {}).get('is_private_session')}")
                    # This is a known Spotify API issue - item returns null for podcasts
                    # Try to get recently played items as fallback to find episode info
                    try:
                        sp_alt = spotipy.Spotify(auth=access_token)
                        
                        # First try currently_playing endpoint
                        currently_playing = sp_alt.currently_playing()
                        if currently_playing and currently_playing.get('item'):
                            print(f"DEBUG: Found item via currently_playing: {currently_playing['item'].get('name')}")
                            current['item'] = currently_playing['item']
                        else:
                            # Try recent tracks to get the most recent episode
                            print(f"DEBUG: Trying recently played tracks to find episode...")
                            recent_tracks = sp_alt.current_user_recently_played(limit=10)
                            
                            if recent_tracks and recent_tracks.get('items'):
                                # Look for the most recent episode that matches the current playing type
                                for recent_item in recent_tracks['items']:
                                    track = recent_item.get('track', {})
                                    if (track.get('type') == 'episode' and 
                                        currently_playing_type == 'episode'):
                                        print(f"DEBUG: Found recent episode: {track.get('name')}")
                                        current['item'] = track
                                        break
                                
                                if not current.get('item'):
                                    print(f"DEBUG: No matching episode found in recent tracks")
                            else:
                                print(f"DEBUG: No recent tracks available")
                            
                            print(f"DEBUG: Full current data: {current}")
                    except Exception as alt_error:
                        print(f"DEBUG: Alternative method failed: {alt_error}")
                        print(f"DEBUG: Full current data: {current}")
            else:
                print("DEBUG: No current playback data")
            
            if not current:
                return None
            
            # Handle case where item is None but we know it's playing something
            item = current.get('item')
            currently_playing_type = current.get('currently_playing_type')
            progress_ms = current.get('progress_ms', 0)
            is_playing = current.get('is_playing', False)
            
            # If we have progress but no item, create a placeholder response
            if not item and currently_playing_type and progress_ms > 0:
                print(f"DEBUG: Creating placeholder for {currently_playing_type}")
                return self._format_placeholder_playback(current, currently_playing_type)
            
            if not item:
                return None
            
            # Check if it's a podcast episode
            if item.get('type') == 'episode':
                return self._format_podcast_playback(current, item)
            elif item.get('type') == 'track':
                # Also support music tracks for testing
                return self._format_track_playback(current, item)
            else:
                # Unknown type
                logger.warning(f"Unknown Spotify item type: {item.get('type')}")
                return None
                
        except Exception as e:
            logger.error(f"Error getting Spotify playback: {e}")
            return None
    
    def _format_podcast_playback(self, current: Dict, episode: Dict) -> Dict[str, Any]:
        """Format podcast playback information."""
        show = episode.get('show', {})
        
        return {
            'is_playing': current.get('is_playing', False),
            'progress_ms': current.get('progress_ms', 0),
            'episode_id': episode.get('id'),
            'episode_name': episode.get('name'),
            'episode_description': episode.get('description'),
            'duration_ms': episode.get('duration_ms'),
            'podcast_name': show.get('name'),
            'podcast_publisher': show.get('publisher'),
            'podcast_description': show.get('description'),
            'episode_uri': episode.get('uri'),
            'release_date': episode.get('release_date'),
            'images': episode.get('images', [])
        }
    
    def _format_track_playback(self, current: Dict, track: Dict) -> Dict[str, Any]:
        """Format music track playback information."""
        artists = track.get('artists', [])
        artist_name = artists[0].get('name') if artists else 'Unknown Artist'
        
        return {
            'is_playing': current.get('is_playing', False),
            'progress_ms': current.get('progress_ms', 0),
            'episode_id': track.get('id'),
            'episode_name': track.get('name'),
            'episode_description': f"Track by {artist_name}",
            'duration_ms': track.get('duration_ms'),
            'podcast_name': f"🎵 Music - {artist_name}",
            'podcast_publisher': artist_name,
            'podcast_description': f"Music track by {artist_name}",
            'episode_uri': track.get('uri'),
            'release_date': track.get('album', {}).get('release_date'),
            'images': track.get('album', {}).get('images', [])
        }
    
    def _format_placeholder_playback(self, current: Dict, content_type: str) -> Dict[str, Any]:
        """Format placeholder playback when item data is missing (Spotify API bug)."""
        device = current.get('device', {})
        device_name = device.get('name', 'Unknown Device')
        progress_ms = current.get('progress_ms', 0)
        
        # Convert progress to readable time
        progress_min = progress_ms // 60000
        progress_sec = (progress_ms % 60000) // 1000
        time_display = f"{progress_min:02d}:{progress_sec:02d}"
        
        return {
            'is_playing': current.get('is_playing', False),
            'progress_ms': progress_ms,
            'episode_id': 'unknown',
            'episode_name': f'Podcast Episode at {time_display}',
            'episode_description': f'Recording bookmark at {time_display}',
            'duration_ms': 0,
            'podcast_name': f'🎙️ Active Podcast',
            'podcast_publisher': 'Spotify Podcast',
            'podcast_description': f'Podcast playing on {device_name}',
            'episode_uri': '',
            'release_date': None,
            'images': []
        }
    
    def get_episode_details(self, access_token: str, episode_id: str) -> Optional[Dict[str, Any]]:
        """Get detailed episode information."""
        try:
            sp = spotipy.Spotify(auth=access_token)
            episode = sp.episode(episode_id)
            return self._format_episode_details(episode)
        except Exception as e:
            logger.error(f"Error getting episode details: {e}")
            return None
    
    def _format_episode_details(self, episode: Dict) -> Dict[str, Any]:
        """Format episode details."""
        show = episode.get('show', {})
        
        return {
            'id': episode.get('id'),
            'name': episode.get('name'),
            'description': episode.get('description'),
            'duration_ms': episode.get('duration_ms'),
            'release_date': episode.get('release_date'),
            'uri': episode.get('uri'),
            'external_urls': episode.get('external_urls', {}),
            'images': episode.get('images', []),
            'podcast': {
                'name': show.get('name'),
                'publisher': show.get('publisher'),
                'description': show.get('description'),
                'images': show.get('images', [])
            }
        }


# Global service instance
spotify_service = SpotifyService()