# 代码清理和改进计划 - 2025-08-26 14:32:00

## 🧹 里程碑后代码清理计划

### ❌ 需要清理的临时解决方案

#### 1. 硬编码的Google Client ID (高优先级)
**位置**: `/android/app/src/main/java/com/poma/ui/screens/LoginScreen.kt:62`
```kotlin
// 当前硬编码
.requestIdToken("882585452174-e4ehsoof2jm9ccs3olh16t5gk9mtokkq.apps.googleusercontent.com")

// 改进方案
.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
```

**实施步骤**:
1. 在 `build.gradle` 中添加 buildConfigField
2. 从硬编码改为构建时配置
3. 添加不同环境的支持 (debug/release)

#### 2. 网络配置临时方案 (中优先级)
**当前**: ADB端口转发 + localhost
**目标**: 支持多环境配置

```kotlin
// 当前硬编码
private val baseUrl = "http://localhost:8001/api/v1/"

// 改进方案
private val baseUrl = BuildConfig.API_BASE_URL
```

#### 3. 简化的TokenManager (中优先级)
**位置**: `/android/app/src/main/java/com/poma/viewmodel/AuthViewModel.kt:154`
```kotlin
// TODO: 实现完整的Token管理
// 当前: 简化版本
// 需要: 刷新令牌、过期处理、安全存储
```

### 📋 待实现的TODO项目

#### Android端 TODO清单
```kotlin
// LoginScreen.kt:172
onClick = { /* TODO: 实现邮箱登录 */ }

// HomeScreen.kt:39  
onClick = { /* TODO: 触发快速书签 */ }

// SettingsScreen.kt:37
onClick = { /* TODO: Spotify 授权 */ }

// BookmarksScreen.kt:27
// TODO: 实现书签列表

// AuthViewModel.kt:29
private val apiService = AuthApiService() // TODO: 注入依赖
```

#### 后端TODO清单
```python
# auth.py
# TODO: 实现用户注册逻辑 (line 39)
# TODO: 实现用户登录逻辑 (line 44) 
# TODO: 从数据库获取或创建用户 (line 64)
# TODO: 实现获取当前用户信息 (line 89)

# bookmarks.py
# TODO: 实现创建书签逻辑 (line 30)
# TODO: 实现音频文件上传和转录 (line 43)
# TODO: 实现获取书签列表 (line 48)
# TODO: 实现获取单个书签 (line 53)
# TODO: 实现删除书签 (line 58)

# spotify.py  
# TODO: 实现 Spotify OAuth 授权 URL 生成 (line 21)
# TODO: 实现 Spotify OAuth 回调处理 (line 27)
# TODO: 实现获取当前播放状态 (line 32)
```

### 🔧 技术债务清理

#### 1. 依赖注入改进
```kotlin
// 当前: 手动创建服务
private val apiService = AuthApiService()

// 目标: 使用依赖注入
@Inject lateinit var apiService: AuthApiService
```

#### 2. 错误处理完善
```kotlin
// 当前: 基础错误显示
authViewModel.setError("登录失败 请稍后重试")

// 目标: 用户友好的错误处理
when (error.code) {
    401 -> "认证失效，请重新登录"
    500 -> "服务器繁忙，请稍后重试"
    // ...
}
```

#### 3. 配置管理规范化
```kotlin
// build.gradle - 添加环境配置
android {
    buildTypes {
        debug {
            buildConfigField "String", "API_BASE_URL", '"http://localhost:8001/api/v1/"'
            buildConfigField "String", "GOOGLE_WEB_CLIENT_ID", '"882585452...com"'
        }
        release {
            buildConfigField "String", "API_BASE_URL", '"https://api.poma.app/v1/"'
            buildConfigField "String", "GOOGLE_WEB_CLIENT_ID", '"882585452...com"'
        }
    }
}
```

### 📦 生产环境准备

#### 安全配置清理
1. **移除硬编码密钥**: 所有敏感信息通过环境变量
2. **网络安全**: 移除cleartext HTTP配置  
3. **调试日志**: 移除生产环境不需要的详细日志

#### 性能优化
1. **代码混淆**: 启用 ProGuard/R8
2. **资源优化**: 移除未使用的资源
3. **APK大小**: 分析和优化包体积

### 🎯 Phase 1B 准备工作

#### 即将开发的功能模块
1. **HomeScreen 完善**: 用户信息显示、登出功能
2. **书签列表基础**: 空状态、加载状态
3. **设置页面**: 基础用户偏好

#### 优先级排序
1. **高**: 硬编码Client ID → BuildConfig
2. **高**: 完善错误处理和用户体验
3. **中**: TokenManager完整实现
4. **中**: 依赖注入架构
5. **低**: 生产环境配置优化

### 🔄 持续改进计划

#### 代码质量
- [ ] 添加单元测试覆盖
- [ ] 设置CI/CD流水线
- [ ] 代码风格统一 (ktlint)
- [ ] 文档完善和同步

#### 架构演进
- [ ] Repository模式实现
- [ ] 本地缓存策略
- [ ] 离线功能支持
- [ ] 数据库集成准备

---

## ✅ 清理完成标准

### 代码质量检查
- [ ] 无硬编码敏感信息
- [ ] 所有TODO标记有跟踪issue
- [ ] 错误处理覆盖主要场景
- [ ] 日志级别适合生产环境

### 架构完善检查  
- [ ] 依赖注入正确实现
- [ ] 配置管理规范化
- [ ] 网络层抽象合理
- [ ] 状态管理清晰

### 文档更新检查
- [ ] README反映当前状态
- [ ] API文档同步
- [ ] 部署文档更新
- [ ] 测试指南完善

---

**预计清理时间**: 4-6小时
**建议完成时间**: Phase 1B开发前完成