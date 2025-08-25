# Poma 开发指南

## 🚀 快速开始

### 主控制脚本
```bash
# 查看所有可用命令
./poma help

# 开发模式 (推荐)
./poma dev

# 启动后端服务器
./poma start-backend

# 构建Android应用
./poma build-android

# 查看服务状态
./poma status

# 停止后端服务器
./poma stop-backend
```

## 📁 项目结构
```
Poma/
├── poma                 # 🎯 主控制脚本
├── scripts/             # 📜 所有开发脚本
│   ├── start-backend.sh # 启动后端脚本
│   ├── stop-backend.sh  # 停止后端脚本
│   └── build-android.sh # 构建Android脚本
├── android/             # 📱 Android 客户端
├── backend/             # 🐍 Python FastAPI 后端
├── docs/                # 📚 项目文档
├── DEVELOPMENT.md       # 🔧 开发指南
└── README.md           # 📖 项目说明
```

## 🔧 开发环境

### 后端开发
```bash
# 进入后端目录
cd backend

# 激活虚拟环境
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 启动开发服务器
uvicorn app.main:app --reload --port 8001
```

### Android 开发
```bash
# 进入 Android 目录
cd android

# 构建应用
./gradlew assembleDebug

# 清理构建
./gradlew clean
```

## 🌐 API 端点

### 基础端点
- **根路径**: http://localhost:8001/
- **健康检查**: http://localhost:8001/health
- **API 文档**: http://localhost:8001/docs

### 认证端点
- **Google 登录**: `POST /api/v1/auth/google`
- **获取用户信息**: `GET /api/v1/auth/me`

### 书签端点
- **创建书签**: `POST /api/v1/bookmarks`
- **获取书签列表**: `GET /api/v1/bookmarks`

### Spotify 集成
- **获取授权URL**: `GET /api/v1/spotify/auth-url`
- **获取播放状态**: `GET /api/v1/spotify/current-playback`

## 🐛 常见问题

### 端口冲突
如果 8001 端口被占用，可以修改 `start-backend.sh` 中的端口号：
```bash
uvicorn app.main:app --reload --port 8002
```

### 虚拟环境问题
删除并重新创建虚拟环境：
```bash
rm -rf backend/venv
./start-backend.sh
```

### Android 构建失败
清理并重新构建：
```bash
cd android
./gradlew clean
./gradlew assembleDebug
```

## 📝 配置文件

### 后端配置 (.env)
```env
# Google OAuth
GOOGLE_CLIENT_ID=你的Web客户端ID

# JWT 配置
SECRET_KEY=你的密钥

# 调试模式
DEBUG=true
```

### Android 配置
- `google-services.json` 必须放在 `android/app/` 目录
- 确保 SHA-1 指纹正确配置

## 🔍 调试技巧

### 查看后端日志
后端启动后会显示详细日志，包括请求信息

### 测试 API
```bash
# 测试根路径
curl http://localhost:8001/

# 测试健康检查
curl http://localhost:8001/health
```

### Android 调试
使用 Android Studio 连接真机或模拟器进行调试