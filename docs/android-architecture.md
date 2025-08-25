# Android 项目架构详解

## 整体架构模式

Poma Android 客户端采用 **MVVM (Model-View-ViewModel)** + **Clean Architecture** 设计模式。

```
┌─────────────────────────────────────────┐
│                UI 层                    │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │  Compose UI │  │    Navigation       ││
│  │  (Screens)  │  │                     ││
│  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│              ViewModel 层                │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │   状态管理   │  │     业务逻辑         ││
│  │ (StateFlow) │  │   (Use Cases)       ││
│  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│               数据层                     │
│  ┌─────────────┐  ┌─────────────────────┐│
│  │ Repository  │  │  Data Sources       ││
│  │   (统一接口) │  │ (API + Local DB)    ││
│  └─────────────┘  └─────────────────────┘│
└─────────────────────────────────────────┘
```

## 核心技术组件

### 1. UI 层 (ui/)
- **Jetpack Compose**: 现代化 UI 声明式开发
- **Material Design 3**: Google 最新设计语言
- **Navigation Compose**: 页面导航管理

```kotlin
// 例子：声明式 UI
@Composable
fun HomeScreen() {
    Column {
        Text("欢迎使用 Poma")
        Button(onClick = { /* 触发书签 */ }) {
            Text("快速书签")
        }
    }
}
```

### 2. ViewModel 层
- **Lifecycle-aware**: 自动处理生命周期
- **StateFlow**: 响应式状态管理
- **Coroutines**: 异步操作处理

```kotlin
class BookmarkViewModel : ViewModel() {
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks
    
    fun createBookmark(note: String) {
        viewModelScope.launch {
            // 异步操作
        }
    }
}
```

### 3. 数据层 (data/)
- **Repository Pattern**: 统一数据访问接口
- **Retrofit**: HTTP 网络请求
- **Room**: 本地数据库存储

## 关键服务组件

### 1. MediaListenerService
```kotlin
// 监听播放状态变化
class MediaListenerService : MediaBrowserServiceCompat() {
    override fun onGetRoot(...) {
        // 获取 Spotify 播放信息
    }
}
```

### 2. QuickBookmarkTileService
```kotlin
// Quick Settings 快捷开关
class QuickBookmarkTileService : TileService() {
    override fun onClick() {
        // 触发快速书签功能
    }
}
```

### 3. AudioRecordingService
```kotlin
// 后台录音服务
class AudioRecordingService : Service() {
    fun startRecording() {
        // 开始录音，显示通知
    }
}
```

## 权限和硬件集成

### 必需权限
```xml
<!-- 录音权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<!-- 媒体控制权限 -->
<uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />
```

### 硬件集成
1. **Quick Tap**: 通过 `AccessibilityService` 监听背部双击
2. **Pixel Buds Pro**: 通过 `MediaSessionCompat` 监听耳机手势
3. **Quick Settings**: 系统下拉菜单快捷开关

## 数据流向

```
用户操作 → ViewModel → Repository → API/Database → UI 更新
```

**举例：创建书签流程**
1. 用户长按 Pixel Buds Pro
2. `MediaListenerService` 监听到手势
3. 启动 `AudioRecordingService` 录音
4. 调用 `BookmarkViewModel.createBookmark()`
5. `BookmarkRepository` 上传音频到后端
6. 后端返回转录结果
7. UI 自动更新显示新书签

## 项目文件结构
```
app/src/main/java/com/poma/
├── ui/
│   ├── screens/          # 页面 Composable
│   ├── components/       # 可复用 UI 组件
│   ├── theme/           # 主题配置
│   └── navigation/      # 导航配置
├── data/
│   ├── api/            # Retrofit API 接口
│   ├── local/          # Room 数据库
│   └── repository/     # 数据仓储
├── service/            # 系统服务
├── utils/              # 工具类
└── MainActivity.kt     # 应用入口
```

这种架构的优点：
- **可测试性**: 每一层都可以独立测试
- **可维护性**: 职责分离，易于修改
- **可扩展性**: 新功能容易添加
- **响应式**: UI 自动响应数据变化