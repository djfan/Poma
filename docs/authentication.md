# Poma 登录认证方案

## 支持的登录方式

### 1. 主要登录方式 (推荐)

#### Google 登录 (Google OAuth 2.0)
- **优势**: Pixel 用户体验最佳，一键登录
- **实现**: 使用 Google Sign-In SDK
- **用户体验**: 点击一次，自动使用 Google 账号登录
- **安全性**: Google 官方认证，无需存储密码

```kotlin
// Android 实现示例
GoogleSignIn.getSignedInAccountFromIntent(data)
    .addOnSuccessListener { account ->
        // 获取 ID Token，发送到后端验证
        val idToken = account.idToken
        authenticateWithBackend(idToken)
    }
```

#### 邮箱 + 密码登录
- **优势**: 传统方式，适合不想使用第三方登录的用户
- **实现**: 自建认证系统
- **功能**: 注册、登录、找回密码

### 2. 扩展登录方式 (未来支持)

#### Spotify 直接登录
- **场景**: 用户已经有 Spotify 账号，直接用 Spotify 登录
- **优势**: 减少注册步骤，直接获得播放权限
- **实现**: Spotify OAuth 2.0

#### Apple ID 登录 (iOS 版本)
- **场景**: 如果未来开发 iOS 版本
- **优势**: Apple 生态用户体验

## 登录流程设计

### 首次使用流程
```
打开 App → 欢迎页面 → 选择登录方式 → 授权 → 主页面
```

### 登录页面 UI 设计
```
┌─────────────────────────┐
│      欢迎使用 Poma       │
│   播客版 Kindle Highlights │
│                        │
│  ┌─────────────────────┐│
│  │  🔍 使用 Google 登录  ││
│  └─────────────────────┘│
│                        │
│  ┌─────────────────────┐│
│  │   📧 邮箱密码登录     ││
│  └─────────────────────┘│
│                        │
│    还没有账号？立即注册    │
└─────────────────────────┘
```

## 后端认证实现

### JWT Token 方案
```python
# FastAPI 后端实现
@router.post("/login/google")
async def google_login(google_token: str):
    # 验证 Google ID Token
    user_info = verify_google_token(google_token)
    
    # 创建或更新用户
    user = get_or_create_user(user_info)
    
    # 生成 JWT Token
    access_token = create_access_token(user_id=user.id)
    
    return {"access_token": access_token, "token_type": "bearer"}
```

### 数据库用户表
```sql
users 表:
- id (主键)
- email (唯一)
- google_id (可选，Google 登录用户)
- spotify_user_id (可选，关联 Spotify 账号)
- created_at, updated_at
- is_active (账号状态)
```

## 安全考虑

### 1. Token 管理
- **Access Token**: 有效期 30 分钟
- **Refresh Token**: 有效期 30 天
- **自动刷新**: 客户端自动处理 token 刷新

### 2. 本地存储
- 使用 Android `EncryptedSharedPreferences` 加密存储
- 不在本地存储密码，只存储 token

### 3. 网络安全
- HTTPS 通信
- Certificate Pinning (证书绑定)

## 用户体验优化

### 1. 记住登录状态
- 用户登录一次后，App 重启仍保持登录状态
- Token 过期时自动刷新

### 2. 离线支持
- 基础功能支持离线使用
- 联网后自动同步数据

### 3. 账号关联
- 登录后可以关联 Spotify 账号
- 一个 Poma 账号可以绑定多个服务

## 实现优先级

### Phase 1 (MVP)
1. ✅ Google 登录 (推荐，Pixel 用户友好)
2. ✅ 邮箱密码登录 (备选)
3. ✅ JWT Token 认证

### Phase 2 (扩展)
1. Spotify 直接登录
2. 社交登录 (GitHub, Twitter)
3. 企业单点登录 (如果有企业用户需求)

## 为什么选择这些方式？

1. **Google 登录**: Pixel 用户最自然的选择，减少摩擦
2. **邮箱登录**: 照顾不想使用第三方登录的用户
3. **简化选择**: 不提供太多登录方式，避免选择困难
4. **安全优先**: 使用成熟的 OAuth 2.0 标准
5. **用户体验**: 一次登录，长期使用