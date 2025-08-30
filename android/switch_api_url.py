#!/usr/bin/env python3
"""
Script to switch Android app between local and cloud API endpoints.
Usage: 
  python switch_api_url.py local    # Use localhost:8001
  python switch_api_url.py cloud    # Use your-app.onrender.com
  python switch_api_url.py https://your-custom-url.com  # Custom URL
"""
import sys
import os
import re
from pathlib import Path

def update_api_urls(target_url):
    """Update all API URLs in Kotlin files."""
    
    # Define the mapping
    if target_url == "local":
        base_url = "http://localhost:8001/"
    elif target_url == "cloud":
        # You'll need to replace this with your actual Render URL after deployment
        base_url = "https://your-app-name.onrender.com/"
    else:
        # Custom URL provided
        base_url = target_url if target_url.endswith('/') else target_url + '/'
    
    api_url = base_url + "api/v1/"
    
    # Find all Kotlin files that contain API URLs
    kotlin_files = [
        "app/src/main/java/com/poma/viewmodel/AuthViewModel.kt",
        "app/src/main/java/com/poma/viewmodel/PlaybackViewModel.kt", 
        "app/src/main/java/com/poma/viewmodel/SpotifyViewModel.kt",
        "app/src/main/java/com/poma/viewmodel/BookmarksViewModel.kt",
        "app/src/main/java/com/poma/viewmodel/VoiceRecordingViewModel.kt"
    ]
    
    updated_files = []
    
    for file_path in kotlin_files:
        if os.path.exists(file_path):
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Update baseUrl patterns
            if 'private val baseUrl' in content:
                content = re.sub(
                    r'private val baseUrl = ".*?"',
                    f'private val baseUrl = "{api_url}"',
                    content
                )
            elif '.baseUrl(' in content:
                content = re.sub(
                    r'\.baseUrl\(".*?"\)',
                    f'.baseUrl("{base_url}")',
                    content
                )
            
            with open(file_path, 'w') as f:
                f.write(content)
            
            updated_files.append(file_path)
            print(f"✅ Updated {file_path}")
    
    print(f"\n🎯 API URLs updated to: {base_url}")
    print(f"📱 Updated {len(updated_files)} files")
    print(f"\nNext steps:")
    print(f"1. cd android")
    print(f"2. ./gradlew assembleDebug")
    print(f"3. adb install -r app/build/outputs/apk/debug/app-debug.apk")
    
    if target_url == "local":
        print(f"4. adb reverse tcp:8001 tcp:8001  # For local development")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python switch_api_url.py [local|cloud|custom-url]")
        print("Examples:")
        print("  python switch_api_url.py local")
        print("  python switch_api_url.py cloud")
        print("  python switch_api_url.py https://my-app.onrender.com")
        sys.exit(1)
    
    target = sys.argv[1]
    update_api_urls(target)