# Android 应用测试指南

## 概述
本指南详细说明如何在 Pixel 手机上测试 Poma Android 应用，包括 Google 登录功能的完整测试流程。

## 前置准备

### 1. 开发环境配置
- **电脑端**：Mac/Linux 系统，已安装 `adb` 工具
- **手机端**：Pixel 7a 或其他 Android 设备
- **网络**：手机和电脑连接同一 WiFi 网络
- **后端服务**：确保后端服务运行在 `http://localhost:8001`

### 2. 安装 ADB 工具（如果尚未安装）
```bash
# 使用 Homebrew 安装
brew install android-platform-tools

# 验证安装
adb --version
```

## 手机端设置步骤

### 1. 启用开发者选项
1. 打开 **Settings** → **About phone**
2. 找到 **Build number** 
3. 连续点击 **Build number** 7次
4. 看到 "You are now a developer!" 提示

### 2. 开启 USB 调试
1. 返回 **Settings** 主页面
2. 找到 **Developer options**（通常在 System 或 Advanced 里）
3. 开启 **USB debugging**

### 3. 连接设备
1. 用 USB 线连接手机到电脑
2. 手机弹出 "Allow USB debugging?" 
3. 勾选 "Always allow from this computer"
4. 点击 **Allow**

### 4. 验证连接
```bash
adb devices
```
**正确输出应该是：**
```
List of devices attached
36141JEHN14814  device
```

## 应用安装和测试

### 1. 构建最新版本
```bash
cd /Users/djfan/Workspace/Poma
./poma build-android
```

### 2. 安装到设备
```bash
# 首次安装
adb install android/app/build/outputs/apk/debug/app-debug.apk

# 更新安装（覆盖旧版本）
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### 3. 网络配置验证
确认电脑和手机在同一网络：
```bash
# 获取电脑 IP 地址
ifconfig | grep "inet " | grep -v 127.0.0.1
```
**示例输出：** `192.168.1.41`

## Google 登录测试流程

### 1. 启动应用
- 在手机上找到 **Poma** 应用图标
- 点击启动应用

### 2. 测试登录功能
1. 应用打开后应显示登录界面
2. 点击 **Sign in with Google** 按钮
3. 选择你的 Gmail 账户
4. 完成 Google 授权流程
5. 应该成功跳转到主页面

### 3. 验证登录状态
- 检查是否显示用户信息
- 尝试导航到不同页面（Home, Bookmarks, Settings）
- 验证应用功能正常

## 故障排除

### 常见问题及解决方案

#### 1. 设备显示 "unauthorized"
```bash
adb kill-server
adb start-server
adb devices
```
然后检查手机是否有授权弹窗。

#### 2. Google 登录无响应
- 确保手机和电脑在同一 WiFi 网络
- 检查后端服务是否运行：访问 `http://localhost:8001`
- 查看后端日志是否接收到请求

#### 3. 网络连接问题
应用已配置使用电脑实际 IP 地址：`192.168.1.41:8001`
如果 IP 地址变化，需要更新 `AuthViewModel.kt` 中的 `baseUrl`。

#### 4. 查看应用日志
```bash
# 查看应用特定日志
adb logcat | grep -i poma

# 查看所有日志（信息较多）
adb logcat
```

#### 5. 查看后端日志
后端服务会实时显示接收到的请求：
```
INFO: 127.0.0.1:xxxxx - "POST /api/v1/auth/google HTTP/1.1" 200 OK
```

## 测试检查清单

### 基本功能测试
- [ ] 应用成功安装并启动
- [ ] 登录界面正确显示
- [ ] Google 登录按钮可点击
- [ ] 能够选择 Google 账户
- [ ] 成功完成登录流程
- [ ] 登录后跳转到主页面

### 导航测试
- [ ] 底部导航栏正确显示
- [ ] Home 页面可访问
- [ ] Bookmarks 页面可访问  
- [ ] Settings 页面可访问
- [ ] 页面间切换正常

### 网络通信测试
- [ ] 后端接收到 Google 登录请求
- [ ] 后端返回正确的用户信息
- [ ] JWT Token 正确保存和使用
- [ ] 用户信息正确显示

## 技术细节

### 网络配置
- **后端地址**：`http://192.168.1.41:8001`
- **网络安全配置**：允许 HTTP 连接到本地 IP
- **权限**：INTERNET, ACCESS_NETWORK_STATE

### Google OAuth 配置
- **Client ID**：`882585452174-msrfafbhd66gmsermrjl46loa9ioeet6.apps.googleusercontent.com`
- **授权范围**：email, profile, openid
- **回调处理**：后端 `/api/v1/auth/google` 端点

### 构建信息
- **APK 位置**：`android/app/build/outputs/apk/debug/app-debug.apk`
- **APK 大小**：约 10M
- **最低 Android 版本**：API 26 (Android 8.0)
- **目标 Android 版本**：API 34

## 相关文档
- [项目开发日志](../log.txt)
- [后端 API 文档](http://localhost:8001/docs)
- [项目架构说明](../ARCHITECTURE.md)

---

**最后更新**：2025-08-26
**测试平台**：Pixel 7a, Android 14
**开发环境**：macOS, adb version 35.0.1