#!/bin/bash
# POMA Backend Start Script

echo "Starting POMA Backend Server..."

cd "$(dirname "$0")/../backend"

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "Error: .env file not found. Please create it with required environment variables."
    exit 1
fi

# Check if virtual environment exists, create if needed with Python 3.11
if [ ! -d "venv" ]; then
    echo "Creating Python 3.11 virtual environment..."
    if ! python3.11 -m venv venv; then
        echo "Error: Could not create virtual environment with Python 3.11"
        echo "Please ensure Python 3.11 is installed: brew install python@3.11"
        exit 1
    fi
    
    echo "Installing dependencies..."
    source venv/bin/activate
    pip install --upgrade pip
    pip install -r requirements.txt
else
    # Activate virtual environment
    source venv/bin/activate
fi

# Check if ADB port forwarding is set up
echo "Setting up ADB port forwarding..."
adb forward tcp:8001 tcp:8001

# Set environment variables
export PYTHONPATH=/Users/djfan/Workspace/Poma/backend
export DATABASE_URL=postgresql://postgres@localhost:5432/poma_dev

# Start the server
echo "Backend server starting on http://0.0.0.0:8001"
echo "Android should connect to http://localhost:8001 (via ADB port forwarding)"
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
