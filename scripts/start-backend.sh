#!/bin/bash

# Poma Backend 一键启动脚本
# 使用方法：./start-backend.sh

echo "🚀 启动 Poma 后端服务器..."

# 获取脚本所在目录并进入项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT/backend"

# 检查虚拟环境是否存在
if [ ! -d "venv" ]; then
    echo "📦 虚拟环境不存在，正在创建..."
    python -m venv venv
    echo "✅ 虚拟环境创建完成"
fi

# 激活虚拟环境
echo "🔧 激活虚拟环境..."
source venv/bin/activate

# 检查依赖是否安装
echo "📋 检查并安装依赖..."
pip install -r requirements.txt

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo "⚠️  .env 文件不存在，请确保已配置环境变量"
    echo "   参考 .env.example 创建 .env 文件"
fi

# 启动服务器
echo "🌐 启动 FastAPI 服务器..."
echo "   访问地址: http://localhost:8001"
echo "   API 文档: http://localhost:8001/docs"
echo "   按 Ctrl+C 停止服务器"
echo ""

# 使用 8001 端口避免冲突
uvicorn app.main:app --reload --port 8001