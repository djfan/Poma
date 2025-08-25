#!/bin/bash

# Poma Android 构建脚本
# 使用方法：./build-android.sh

echo "📱 构建 Poma Android 应用..."

# 获取脚本所在目录并进入项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT/android"

# 检查 gradlew 是否存在
if [ ! -f "gradlew" ]; then
    echo "❌ gradlew 不存在，请检查 Android 项目设置"
    exit 1
fi

# 给 gradlew 执行权限
chmod +x gradlew

# 清理之前的构建
echo "🧹 清理之前的构建..."
./gradlew clean

# 构建 Debug 版本
echo "🔨 构建 Debug APK..."
./gradlew assembleDebug

# 检查构建结果
if [ $? -eq 0 ]; then
    echo "✅ Android 应用构建成功！"
    echo "📦 APK 位置: android/app/build/outputs/apk/debug/app-debug.apk"
    
    # 显示 APK 信息
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
        echo "📊 APK 大小: $APK_SIZE"
    fi
else
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

echo "🏁 完成"