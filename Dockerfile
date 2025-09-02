FROM python:3.11.9-slim

WORKDIR /app

# Copy requirements first for better caching
COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the backend code
COPY backend/ .

# Expose port
EXPOSE $PORT

# Start the application
CMD uvicorn app.main:app --host 0.0.0.0 --port $PORT