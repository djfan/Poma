# 🚀 Poma Cloud Deployment Guide

## Quick Cloud Options Comparison

| Platform | Cost | Setup Time | Database | SSL | Best For |
|----------|------|------------|----------|-----|----------|
| **🥇 Railway** | $5/month | 5 minutes | ✅ Managed | ✅ Auto | **Recommended** |
| **🔥 Render** | Free tier | 10 minutes | ✅ Managed | ✅ Auto | Budget-friendly |
| **☁️ Google Cloud** | $20+/month | 30 minutes | ✅ Cloud SQL | ✅ Auto | Professional |
| **🌊 DigitalOcean** | $12/month | 15 minutes | ✅ Managed | ✅ Auto | Balanced |

---

## 🥇 **Option 1: Railway (Recommended)**

### Why Railway?
- ✅ **Simplest deployment** - Just connect GitHub
- ✅ **Managed PostgreSQL** included
- ✅ **Auto HTTPS** with custom domains
- ✅ **$5/month** hobby plan
- ✅ **Zero config** needed

### Step-by-Step Railway Deployment

#### 1. **Prepare Repository**
```bash
# Already done - your repo is ready!
git add railway.toml
git commit -m "Add Railway configuration"
git push origin main
```

#### 2. **Deploy to Railway**
1. Go to [railway.app](https://railway.app)
2. Sign up with GitHub
3. Click **"New Project"** → **"Deploy from GitHub"**
4. Select your `Poma` repository
5. Railway will auto-detect the FastAPI app

#### 3. **Add PostgreSQL Database**
1. In Railway dashboard, click **"New"** → **"Database"** → **"PostgreSQL"**  
2. Railway automatically provides `DATABASE_URL` environment variable

#### 4. **Configure Environment Variables**
In Railway dashboard, add these variables:
```bash
GOOGLE_CLIENT_ID=882585452174-e4ehsoof2jm9ccs3olh16t5gk9mtokkq.apps.googleusercontent.com
SECRET_KEY=your-secure-production-secret-here
DEBUG=false
PORT=8001
```

Optional (for full features):
```bash
OPENAI_API_KEY=your-openai-key
SPOTIFY_CLIENT_ID=your-spotify-client-id
SPOTIFY_CLIENT_SECRET=your-spotify-client-secret
```

#### 5. **Update Android App**
You'll need to update the API base URL in your Android app to use your Railway URL:
```kotlin
// In app/src/main/java/com/poma/api/ApiService.kt
private const val BASE_URL = "https://your-app-name.up.railway.app/api/v1/"
```

#### 6. **Deploy & Test**
- Railway deploys automatically on every git push
- Your app will be available at: `https://your-app-name.up.railway.app`
- API docs at: `https://your-app-name.up.railway.app/docs`

**Total Time: ~10 minutes** ⏱️  
**Monthly Cost: ~$5** 💰

---

## 🔥 **Option 2: Render (Free Tier)**

### Why Render?
- ✅ **Free tier** with 750 hours/month
- ✅ **Managed PostgreSQL** 
- ✅ **Auto SSL** and scaling
- ⚠️ **Cold starts** (app sleeps after 15min idle)

### Deploy to Render
1. Go to [render.com](https://render.com)
2. Connect GitHub repo
3. Choose **"Web Service"**
4. Set build command: `cd backend && pip install -r requirements.txt`
5. Set start command: `cd backend && uvicorn app.main:app --host 0.0.0.0 --port $PORT`
6. Add PostgreSQL database from Render dashboard

**Free Tier Limits:**
- 512MB RAM, 0.1 CPU  
- App sleeps after 15min idle (20-30s cold start)
- 750 build hours/month

---

## ☁️ **Option 3: Google Cloud (Professional)**

### Why Google Cloud?
- ✅ **Native Google OAuth** integration
- ✅ **Serverless scaling** (Cloud Run)
- ✅ **Professional features**
- ✅ **$300 free credits** for new accounts

### Services Needed:
1. **Cloud Run** (FastAPI backend) - $0.40/million requests
2. **Cloud SQL** (PostgreSQL) - ~$25/month minimum  
3. **Cloud Storage** (audio files) - ~$1/month

### Quick Setup:
```bash
# 1. Install Google Cloud CLI
# 2. Deploy with:
gcloud run deploy poma-backend \
    --source=./backend \
    --port=8001 \
    --region=us-central1 \
    --allow-unauthenticated
```

---

## 🌊 **Option 4: DigitalOcean App Platform**

### Why DigitalOcean?
- ✅ **Predictable pricing** $12/month
- ✅ **Good performance** no cold starts
- ✅ **Managed database** add-on
- ✅ **Simple interface**

### Setup:
1. Create account at [digitalocean.com](https://digitalocean.com)
2. Go to **Apps** → **Create App**
3. Connect GitHub repository
4. Add **PostgreSQL** managed database ($15/month)
5. Set environment variables

**Total Cost: ~$27/month** (App $12 + Database $15)

---

## 📱 **Android App Configuration**

After deploying to any cloud platform, you'll need to update your Android app:

### 1. **Update API Base URL**
```kotlin
// In ApiService.kt or similar
private const val BASE_URL = "https://your-cloud-url.com/api/v1/"
```

### 2. **Update Google OAuth Configuration**  
Add your cloud domain to Google Cloud Console OAuth settings:
- Go to Google Cloud Console → APIs & Credentials
- Edit your OAuth 2.0 Client ID
- Add your cloud domain to authorized origins

### 3. **Rebuild and Install APK**
```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 **Recommendation**

**For you**: Start with **Railway** 🥇
- **Fastest setup** (10 minutes)
- **Best value** ($5/month)  
- **Production ready**
- **Easy to migrate** later if needed

### Next Steps:
1. Push current code to GitHub
2. Deploy to Railway following the guide above
3. Update Android app with new URL
4. Test end-to-end functionality

**Your app will be accessible from anywhere in the world!** 🌍