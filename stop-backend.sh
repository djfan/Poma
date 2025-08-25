#!/bin/bash

# Poma Backend 停止脚本
# 使用方法：./stop-backend.sh

echo "🛑 停止 Poma 后端服务器..."

# 查找并停止 uvicorn 进程
PIDS=$(pgrep -f "uvicorn app.main:app")

if [ -z "$PIDS" ]; then
    echo "ℹ️  没有找到运行中的后端服务器"
else
    echo "🔍 找到进程: $PIDS"
    kill $PIDS
    echo "✅ 后端服务器已停止"
fi

# 也可以通过端口停止
PORT_PID=$(lsof -ti:8001)
if [ ! -z "$PORT_PID" ]; then
    echo "🔍 发现端口 8001 被进程 $PORT_PID 占用，正在停止..."
    kill $PORT_PID
    echo "✅ 端口 8001 已释放"
fi

echo "🏁 完成"