#!/bin/bash

# Production startup script for Poma backend

set -e

echo "🚀 Starting Poma Backend - Production Mode"

# Load production environment
if [ -f ".env.production" ]; then
    echo "📋 Loading production environment..."
    export $(cat .env.production | grep -v '^#' | xargs)
else
    echo "⚠️  Warning: .env.production not found"
fi

# Activate virtual environment
echo "🐍 Activating virtual environment..."
source venv/bin/activate

# Set production database URL explicitly
export DATABASE_URL=postgresql://djfan@localhost:5432/poma_production
export DEBUG=False

echo "🗄️  Database: $DATABASE_URL"

# Check database connection
echo "🔍 Testing database connection..."
python -c "
from sqlalchemy import create_engine, text
engine = create_engine('$DATABASE_URL')
with engine.connect() as conn:
    result = conn.execute(text('SELECT current_database()'))
    db_name = result.scalar()
    result = conn.execute(text('SELECT count(*) FROM information_schema.tables WHERE table_schema = \\'public\\''))
    table_count = result.scalar()
    print(f'✅ Connected to: {db_name}')
    print(f'📊 Tables available: {table_count}')
"

# Start the server
echo "🌐 Starting FastAPI server in production mode..."
echo "📍 Server will be available at: http://localhost:8001"
echo "📖 API Documentation: http://localhost:8001/docs"

exec uvicorn app.main:app \
    --host 0.0.0.0 \
    --port 8001 \
    --workers 4 \
    --access-log