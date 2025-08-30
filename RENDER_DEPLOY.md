# 🚀 Deploy Poma to Render (Free)

## Quick Setup Steps

### 1. **Push Code to GitHub**
```bash
git add .
git commit -m "Add Render deployment configuration"
git push origin main
```

### 2. **Deploy to Render**
1. Go to [render.com](https://render.com) and sign up with GitHub
2. Click **"New +"** → **"Web Service"**
3. Connect your `Poma` repository
4. Configure:
   - **Name**: `poma-backend`
   - **Root Directory**: `backend`
   - **Environment**: `Python 3`
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`

### 3. **Add PostgreSQL Database**
1. In Render dashboard, click **"New +"** → **"PostgreSQL"**
2. Name it `poma-postgres`
3. Choose **Free** plan
4. Copy the **Database URL** once created

### 4. **Set Environment Variables**
In your web service settings, add:
```
DATABASE_URL=postgresql://[paste-the-url-from-step-3]
DEBUG=false
SECRET_KEY=[auto-generated-by-render]
GOOGLE_CLIENT_ID=882585452174-e4ehsoof2jm9ccs3olh16t5gk9mtokkq.apps.googleusercontent.com
```

### 5. **Update Android App**
Once deployed, you'll get a URL like `https://poma-backend.onrender.com`

Update your Android app:
```bash
cd android
python switch_api_url.py https://poma-backend.onrender.com
./gradlew assembleDebug  
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6. **Update Google OAuth**
Add your Render domain to Google Cloud Console:
- Go to Google Cloud Console → APIs & Credentials  
- Edit OAuth 2.0 Client ID
- Add `https://poma-backend.onrender.com` to authorized origins

## 🎉 Your app will be live at your Render URL!

**Free Tier Includes:**
- ✅ 750 build hours/month
- ✅ Auto SSL (HTTPS)
- ✅ Custom domain support
- ✅ Automatic deploys on git push
- ⚠️ App sleeps after 15min idle (30s cold start)

**Total Time: ~15 minutes** ⏱️  
**Cost: FREE** 💰