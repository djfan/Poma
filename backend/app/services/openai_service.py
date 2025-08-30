"""OpenAI Whisper service for speech-to-text conversion."""

import os
import tempfile
import subprocess
from typing import Optional
from openai import OpenAI
from app.core.config import settings

class OpenAIService:
    def __init__(self):
        self.client = OpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None
    
    def _convert_to_supported_format(self, input_path: str) -> str:
        """Convert audio file to mp3 format if needed."""
        # Check if file is already in supported format
        supported_extensions = ['.mp3', '.wav', '.ogg', '.webm', '.m4a', '.flac', '.mpeg', '.mpga', '.mp4', '.oga']
        file_ext = os.path.splitext(input_path)[1].lower()
        
        # If it's m4a, convert to mp3 for better Whisper compatibility
        if file_ext == '.m4a':
            output_path = input_path.replace('.m4a', '.mp3')
            try:
                # Use ffmpeg to convert m4a to mp3
                result = subprocess.run([
                    'ffmpeg', '-i', input_path, '-acodec', 'mp3', '-ab', '128k', 
                    output_path, '-y'
                ], capture_output=True, text=True, timeout=30)
                
                if result.returncode == 0:
                    print(f"Converted {input_path} to {output_path}")
                    return output_path
                else:
                    print(f"FFmpeg conversion failed: {result.stderr}")
                    return input_path
            except subprocess.TimeoutExpired:
                print("Audio conversion timed out")
                return input_path
            except Exception as e:
                print(f"Error converting audio: {e}")
                return input_path
        
        return input_path

    async def transcribe_audio(self, audio_file_path: str) -> Optional[str]:
        """
        Convert audio file to text using OpenAI Whisper.
        
        Args:
            audio_file_path: Path to the audio file
            
        Returns:
            Transcribed text or None if transcription fails
        """
        if not self.client:
            print("OpenAI API key not configured, skipping transcription")
            return None
            
        converted_path = None
        try:
            # Convert to supported format if needed
            converted_path = self._convert_to_supported_format(audio_file_path)
            
            # Open the audio file
            with open(converted_path, "rb") as audio_file:
                # Call Whisper API
                transcript = self.client.audio.transcriptions.create(
                    model="whisper-1",
                    file=audio_file,
                    response_format="text"
                )
                
            # Return the transcribed text
            return transcript.strip() if transcript else None
            
        except Exception as e:
            print(f"Error transcribing audio: {e}")
            return None
        finally:
            # Clean up converted file if it was created
            if converted_path and converted_path != audio_file_path and os.path.exists(converted_path):
                try:
                    os.remove(converted_path)
                    print(f"Cleaned up converted file: {converted_path}")
                except Exception as e:
                    print(f"Warning: Could not delete converted file: {e}")

# Global service instance
openai_service = OpenAIService()