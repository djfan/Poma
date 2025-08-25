# Google 登录实现指南

## 第一步：Google Cloud Console 配置 (15分钟)

### 1.1 创建项目
1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 点击 "Select a project" → "New Project"
3. 项目名称：`Poma`，记下项目ID

### 1.2 启用 Google Sign-In API
1. 在侧边栏选择 "APIs & Services" → "Library"
2. 搜索 "Google Sign-In API" 或 "Google+ API"
3. 点击 "Enable"

### 1.3 创建 OAuth 2.0 凭据
1. 转到 "APIs & Services" → "Credentials"
2. 点击 "Create Credentials" → "OAuth client ID"
3. 如果提示配置同意屏幕，先配置：
   - Application type: External
   - App name: Poma
   - User support email: 你的邮箱
   - Authorized domains: localhost
   - Developer contact: 你的邮箱

### 1.4 Android OAuth 客户端
1. Application type 选择 "Android"
2. Package name: `com.poma`
3. SHA-1 证书指纹：需要生成（见下方）

### 1.5 获取 SHA-1 指纹
在项目根目录运行：
```bash
cd android
./gradlew signingReport
```
复制 `SHA1` 值粘贴到 Google Console

### 1.6 创建 Web OAuth 客户端（后端验证用）
1. 再次点击 "Create Credentials" → "OAuth client ID"
2. Application type 选择 "Web application" 
3. Name: "Poma Backend"
4. 记下 Client ID，后端验证需要用到

## 第二步：Android 项目配置 (15分钟)

### 2.1 下载 google-services.json
1. 在 Google Console 的 Credentials 页面
2. 点击下载按钮，下载 `google-services.json`
3. 放到 `android/app/` 目录下

### 2.2 添加依赖
编辑 `android/app/build.gradle`：
```gradle
dependencies {
    // Google Sign-In
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    
    // Google Services plugin
    implementation 'com.google.gms:google-services:4.4.0'
}
```

编辑项目级 `android/build.gradle`：
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

在 `android/app/build.gradle` 底部添加：
```gradle
apply plugin: 'com.google.gms.google-services'
```

### 2.3 添加网络权限
确保 `AndroidManifest.xml` 有网络权限（已添加）

## 第三步：Android 代码实现 (15分钟)

### 3.1 创建登录页面
文件位置：`android/app/src/main/java/com/poma/ui/screens/LoginScreen.kt`

### 3.2 创建认证 ViewModel
文件位置：`android/app/src/main/java/com/poma/viewmodel/AuthViewModel.kt`

### 3.3 更新导航
修改 `PomaNavigation.kt` 添加登录路由

## 第四步：后端 Google Token 验证 (15分钟)

### 4.1 安装依赖
添加到 `backend/requirements.txt`：
```
google-auth==2.25.2
```

### 4.2 环境变量配置
添加到 `.env`：
```
GOOGLE_CLIENT_ID=你的Web客户端ID
```

### 4.3 实现验证逻辑
修改 `backend/app/api/api_v1/endpoints/auth.py`

## 第五步：测试验证 (5分钟)

### 5.1 Android 测试
- 运行 Android 应用
- 点击 Google 登录按钮
- 查看是否弹出 Google 账号选择

### 5.2 后端测试
- 启动后端服务
- 检查 token 验证接口
- 确认用户创建逻辑

## 常见问题解决

### 问题1：SHA-1 指纹不匹配
- 重新生成并更新 Google Console
- 确保使用正确的 keystore

### 问题2：Google 登录无响应
- 检查 `google-services.json` 位置
- 确认包名匹配

### 问题3：Token 验证失败
- 检查 Web 客户端 ID 是否正确
- 确认后端网络连接正常

## 完成检查清单

- [ ] Google Cloud 项目创建
- [ ] OAuth 2.0 凭据配置
- [ ] google-services.json 下载
- [ ] Android 依赖添加
- [ ] 登录页面实现
- [ ] 后端验证逻辑
- [ ] 端到端测试通过

预计总时间：45-60分钟