"""OpenAI Whisper service for speech-to-text conversion."""

import os
import tempfile
from typing import Optional
from openai import OpenAI
from app.core.config import settings

class OpenAIService:
    def __init__(self):
        self.client = OpenAI(api_key=settings.OPENAI_API_KEY) if settings.OPENAI_API_KEY else None
    
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
            
        try:
            # Open the audio file
            with open(audio_file_path, "rb") as audio_file:
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

# Global service instance
openai_service = OpenAIService()