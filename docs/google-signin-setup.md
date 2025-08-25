# Google 登录实现指南

## 第一步：Google Cloud Console 配置 (15分钟)

### 1.1 创建项目
1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 点击项目选择器 → **"New Project"**
3. 项目名称：**`POMA`** (或你喜欢的名字)
4. 点击 **"CREATE"**

### 1.2 配置 OAuth 同意屏幕
⚠️ **重要**：不需要启用特定 API，直接配置 OAuth 即可

1. 左侧菜单：**"APIs & Services"** → **"OAuth consent screen"**
2. 选择 **"External"** → 点击 **"CREATE"**
3. 填写必填信息：
   - **App name**: `Poma`
   - **User support email**: 你的邮箱地址
   - **Developer contact information**: 你的邮箱地址
4. 点击 **"SAVE AND CONTINUE"**
5. 后续页面（Scopes, Test users）直接点 **"SAVE AND CONTINUE"** 跳过
6. 最后点击 **"BACK TO DASHBOARD"**

### 1.3 获取 SHA-1 指纹
在项目根目录运行：
```bash
cd android
./gradlew signingReport
```
复制输出中的 **SHA1** 值（形如：`AA:BB:CC:DD:EE:FF:...`）

**示例输出**：
```
Variant: debug
Config: debug
Store: ~/.android/debug.keystore
Alias: AndroidDebugKey
SHA1: 22:B0:77:DC:81:7B:C7:FF:B6:2D:30:E1:F2:D3:0A:21:2F:A0:23:82  ← 复制这行
```

### 1.4 创建 Android OAuth 客户端
1. 转到：**"APIs & Services"** → **"Credentials"**
2. 点击 **"+ CREATE CREDENTIALS"** → **"OAuth client ID"**
3. 应用类型选择：**"Android"**
4. 填写：
   - **Name**: `Poma Android`
   - **Package name**: `com.poma`
   - **SHA-1 certificate fingerprint**: 粘贴上一步获取的 SHA-1 指纹
5. 点击 **"CREATE"**

### 1.5 创建 Web OAuth 客户端（后端验证用）
1. 再次点击 **"+ CREATE CREDENTIALS"** → **"OAuth client ID"**
2. 应用类型选择：**"Web application"**
3. 填写：
   - **Name**: `Poma Backend`
   - **Authorized redirect URIs**: 留空即可
4. 点击 **"CREATE"**
5. ⚠️ **重要**：记下显示的 **Client ID**（后端配置需要用到）

### 1.6 下载配置文件
1. 在 **Credentials** 页面找到 **"Poma Android"** 客户端
2. 点击右侧的 **下载图标** 📥
3. 将下载的 JSON 文件重命名为 `google-services.json`
4. 放置到：`android/app/google-services.json`

## 第二步：更新项目配置 (5分钟)

### 2.1 更新 Android 代码中的 Web Client ID
编辑文件：`android/app/src/main/java/com/poma/ui/screens/LoginScreen.kt`

找到这一行：
```kotlin
.requestIdToken("YOUR_WEB_CLIENT_ID") // 需要替换为实际的 Web Client ID
```

替换为你的 Web Client ID：
```kotlin
.requestIdToken("123456789-abcdefg.apps.googleusercontent.com") // 你的实际 Web Client ID
```

### 2.2 配置后端环境变量
1. 复制 `backend/.env.example` 为 `backend/.env`：
   ```bash
   cp backend/.env.example backend/.env
   ```

2. 编辑 `backend/.env`，添加你的 Web Client ID：
   ```env
   # Google OAuth 配置
   GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com  # 你的实际 Web Client ID
   
   # JWT 配置
   SECRET_KEY=your-super-secret-key-here-please-change-this
   ```

## 第三步：测试验证 (5分钟)

### 3.1 启动后端服务
```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload
```

后端应该运行在：http://localhost:8000

### 3.2 测试后端 API
在浏览器访问：http://localhost:8000/docs
应该能看到 FastAPI 的自动文档，包含 `/api/v1/auth/google` 接口

### 3.3 构建 Android 应用
```bash
cd android
./gradlew assembleDebug
```

如果构建成功，说明所有依赖和配置都正确。

## 常见问题解决

### 问题1：SHA-1 指纹不匹配
```
Error: Sign in failed (DEVELOPER_ERROR)
```
**解决方案**：
- 重新运行 `./gradlew signingReport` 获取正确的 SHA-1
- 在 Google Console 更新 Android 客户端的 SHA-1 指纹
- 等待 5-10 分钟让配置生效

### 问题2：Google 登录无响应
```
Google Sign-In 按钮没有反应
```
**解决方案**：
- 检查 `google-services.json` 是否在正确位置：`android/app/google-services.json`
- 确认包名匹配：`com.poma`
- 重新构建应用：`./gradlew clean assembleDebug`

### 问题3：Token 验证失败
```
后端返回 "Invalid Google token"
```
**解决方案**：
- 检查 `.env` 文件中的 `GOOGLE_CLIENT_ID` 是否是 **Web 客户端 ID**（不是 Android 客户端 ID）
- 确认后端网络连接正常
- 检查 Web 客户端 ID 格式：应该形如 `123456789-abc123.apps.googleusercontent.com`

### 问题4：Gradle 构建失败
```
Could not resolve com.google.gms:google-services
```
**解决方案**：
- 确保网络连接正常
- 清理 Gradle 缓存：`./gradlew clean`
- 重新下载依赖：`./gradlew --refresh-dependencies`

## 完成检查清单

### Google Cloud Console
- [ ] 项目 POMA 创建成功
- [ ] OAuth 同意屏幕配置完成
- [ ] Android OAuth 客户端创建（包含正确的 SHA-1）
- [ ] Web OAuth 客户端创建（记录 Client ID）
- [ ] 下载 `google-services.json` 文件

### 项目配置  
- [ ] `google-services.json` 放置在 `android/app/` 目录
- [ ] `backend/.env` 配置 Web Client ID
- [ ] Android 代码更新 Web Client ID

### 测试验证
- [ ] 后端服务启动成功（http://localhost:8000）
- [ ] Android 应用构建成功
- [ ] Google 登录按钮能触发账号选择
- [ ] 端到端登录流程正常

**预计总时间：30-45分钟**

## 下一步
配置完成后，就可以开始实现 Spotify API 集成和硬件触发功能了！