# Poma 项目结构说明

## 概览

Poma 项目采用前后端分离架构，包含 Android 客户端和 FastAPI 后端服务。

```
Poma/
├── android/                 # Android 客户端
│   ├── app/
│   │   ├── src/main/java/com/poma/
│   │   │   ├── ui/          # UI 层
│   │   │   │   ├── screens/ # 屏幕组件
│   │   │   │   ├── theme/   # 主题配置
│   │   │   │   └── navigation/ # 导航
│   │   │   ├── data/        # 数据层
│   │   │   │   ├── api/     # API 客户端
│   │   │   │   ├── local/   # 本地存储
│   │   │   │   └── repository/ # 仓储层
│   │   │   ├── service/     # 系统服务
│   │   │   └── utils/       # 工具类
│   │   └── build.gradle
│   └── build.gradle
├── backend/                 # FastAPI 后端
│   ├── app/
│   │   ├── api/             # API 路由
│   │   ├── core/            # 核心配置
│   │   ├── models/          # 数据模型
│   │   └── services/        # 业务逻辑
│   ├── requirements.txt
│   └── .env.example
├── docs/                    # 项目文档
├── brainstorm.txt          # 产品设计思路
├── log.txt                 # 开发日志
└── README.md
```

## Android 客户端架构

### UI 层 (ui/)
- **screens/**: 各个页面的 Compose UI 组件
  - HomeScreen: 主页，快速书签入口
  - BookmarksScreen: 书签列表和回顾
  - SettingsScreen: 设置页面，Spotify 授权
- **theme/**: Material Design 3 主题配置
- **navigation/**: Navigation Compose 路由管理

### 数据层 (data/)
- **api/**: Retrofit API 客户端，与后端通信
- **local/**: Room 数据库本地缓存
- **repository/**: 数据仓储层，统一数据访问

### 服务层 (service/)
- QuickBookmarkTileService: Quick Settings 快捷开关
- MediaListenerService: 监听播放状态
- AudioRecordingService: 语音录制服务

## 后端架构

### API 层 (api/)
- **auth.py**: 用户认证相关接口
- **spotify.py**: Spotify API 集成
- **bookmarks.py**: 书签管理接口

### 模型层 (models/)
- **user.py**: 用户模型，包含 Spotify 授权信息
- **bookmark.py**: 书签模型，包含播客信息和笔记内容

### 服务层 (services/)
- SpotifyService: Spotify API 封装
- AudioService: 音频处理和转录
- AIService: OpenAI API 集成

## 关键特性实现

### 硬件集成
1. **Quick Tap**: 通过 AccessibilityService 监听背部双击
2. **Pixel Buds**: 通过 MediaSession 监听耳机手势
3. **Quick Settings Tile**: 系统下拉快捷开关

### Spotify 集成
1. OAuth 2.0 授权流程
2. 播放状态实时获取
3. 时间戳精确到毫秒级

### 语音处理
1. 录音权限管理
2. 音频文件上传
3. Whisper API 语音转文字

## 数据流

```
用户触发 → 硬件事件 → 获取播放状态 → 录音 → 上传转录 → 保存书签 → AI 处理
```

1. 用户通过硬件触发（Quick Tap / Pixel Buds）
2. Android 应用获取当前 Spotify 播放状态
3. 开始录音，显示录音界面
4. 录音结束，上传到后端
5. 后端调用 Whisper API 转录
6. 保存书签到数据库
7. 可选：AI 生成摘要和思考扩展