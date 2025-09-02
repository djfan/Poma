# Backend Connection Troubleshooting Guide

## Common Issue: Android "Failed to connect to localhost:8001"

### Root Cause Analysis
The Android sign-in connection failure typically stems from:

1. **Backend server not properly running**
2. **Port conflicts from previous sessions** 
3. **ADB port forwarding not configured**
4. **Backend bound to wrong network interface**

### Step-by-Step Resolution Process

#### 1. Check Backend Server Status
```bash
# Test if backend is responding
curl http://localhost:8001/
# Expected: {"message":"Poma API"}
```

#### 2. Kill Conflicting Processes
```bash
# Find and kill any processes using port 8001
lsof -ti:8001 | xargs kill -9
```

#### 3. Verify Working Directory
```bash
# Must be in the backend directory
pwd
# Expected: /Users/djfan/Workspace/Poma/backend
```

#### 4. Start Backend Properly
```bash
# Navigate to backend if needed
cd /Users/djfan/Workspace/Poma/backend

# Set environment variables and start server
export SPOTIFY_CLIENT_ID=your_spotify_client_id
export SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

#### 5. Configure ADB Port Forwarding
```bash
# Forward device port 8001 to computer port 8001
adb forward tcp:8001 tcp:8001

# Verify forwarding is active
adb forward --list
# Expected: [device_id] tcp:8001 tcp:8001
```

#### 6. Verify Connection
```bash
# Test backend response
curl http://localhost:8001/
# Expected: {"message":"Poma API"}

# Test health endpoint
curl http://localhost:8001/health  
# Expected: {"status":"ok"}
```

### Common Pitfalls

❌ **Wrong Directory**: Starting uvicorn from root directory instead of backend/
❌ **Missing Environment Variables**: SPOTIFY credentials not set
❌ **Port Conflicts**: Multiple uvicorn processes running
❌ **Wrong Host Binding**: Using 127.0.0.1 instead of 0.0.0.0
❌ **Missing ADB Forwarding**: Device can't reach localhost

### Quick Diagnostic Commands

```bash
# Check if port 8001 is in use
lsof -i:8001

# Check current directory
pwd

# Check if backend imports correctly  
python -c "import app.main; print('Import successful')"

# Check ADB connection
adb devices

# Check port forwarding
adb forward --list
```

### Success Indicators

✅ Backend server logs show: "Uvicorn running on http://0.0.0.0:8001"
✅ `curl http://localhost:8001/` returns JSON response
✅ `adb forward --list` shows tcp:8001 forwarding
✅ Android app can successfully sign in

### Recovery Time
Following this process should resolve connection issues within 2-3 minutes instead of the 10+ minutes spent troubleshooting ad-hoc.

---

**Last Updated**: 2025-08-27
**Issue Frequency**: Common during development sessions with multiple backend restarts