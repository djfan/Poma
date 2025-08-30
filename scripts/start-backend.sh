#!/bin/bash
# POMA Backend Start Script

echo "Starting POMA Backend Server..."

cd "$(dirname "$0")/../backend"

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "Error: .env file not found. Please create it with required environment variables."
    exit 1
fi

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Error: Virtual environment not found. Please run setup first."
    exit 1
fi

# Activate virtual environment
source venv/bin/activate

# Check if ADB port forwarding is set up
echo "Setting up ADB port forwarding..."
adb forward tcp:8001 tcp:8001

# Start the server
echo "Backend server starting on http://0.0.0.0:8001"
echo "Android should connect to http://localhost:8001 (via ADB port forwarding)"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
