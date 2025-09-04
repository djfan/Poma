FROM python:3.11.9-slim

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Install system dependencies for PostgreSQL and audio processing
RUN apt-get update && apt-get install -y \
    libpq-dev \
    gcc \
    postgresql-client \
    build-essential \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy requirements first for better caching
COPY backend/requirements.txt .

# Install dependencies
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy the backend code
COPY backend/ .

# Create uploads directory for audio files
RUN mkdir -p uploads/audio

# Expose port (this is just documentation, Render uses PORT env var)
EXPOSE 8000

# Start the application
CMD uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}