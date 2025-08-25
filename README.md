# Poma - Podcast Bookmarks & Notes

> The Kindle Highlights for Podcasts - powered by your earbuds.

Poma 是一个轻量级的播客笔记工具，让用户在跑步、通勤等移动场景下能够：
- 通过 Pixel Buds Pro 耳机一键触发语音笔记
- 自动关联到正在播放的播客时间戳  
- 像 Kindle highlights 一样回顾这些笔记

## 项目结构

```
├── android/          # Android 客户端 (Kotlin + Jetpack Compose)
├── backend/           # 后端 API (FastAPI + Python)
├── docs/             # 项目文档
├── brainstorm.txt    # 产品需求和设计思路
└── log.txt          # 开发日志
```

## 核心功能

### Phase 1 - MVP
- [x] 项目架构设计
- [ ] Spotify OAuth 登录
- [ ] 播放状态获取
- [ ] 语音笔记录制
- [ ] 基础回顾界面

### Phase 2 - AI 增强
- [ ] 上下文捕获 (播客前后30秒转录)
- [ ] AI 总结与思考扩展
- [ ] 周期性回顾报告

### Phase 3 - 扩展功能
- [ ] 多平台支持 (Apple Music, 小宇宙)
- [ ] 可视化统计
- [ ] 社区分享

## 硬件集成

- **Pixel Buds Pro**: 长按手势触发笔记
- **Pixel 7a**: Quick Tap (背部双击) 备选触发
- **Android 系统**: Quick Settings Tile, 通知栏快捷操作

## 开发环境要求

### Android
- Android Studio Arctic Fox+
- Kotlin 1.9+
- Compose BOM 2024.02+

### Backend  
- Python 3.11+
- FastAPI 0.104+
- PostgreSQL 15+

## 🚀 快速开始

### 一键启动
```bash
# 查看所有命令
./poma help

# 启动后端服务器
./poma start-backend

# 构建 Android 应用  
./poma build-android

# 开发模式 (启动后端 + 显示文档链接)
./poma dev

# 查看服务状态
./poma status
```

### 手动启动
```bash
# 启动后端
cd backend
source venv/bin/activate
uvicorn app.main:app --reload --port 8001

# 构建 Android 应用
cd android
./gradlew assembleDebug
```

### 访问应用
- **后端 API**: http://localhost:8001
- **API 文档**: http://localhost:8001/docs
- **Android APK**: `android/app/build/outputs/apk/debug/app-debug.apk`

## 测试

参考 [测试指南](docs/testing.md) 了解如何在真实硬件环境下测试。

## License

MIT License