#!/bin/bash
set -e

# DataForge 本地开发启动脚本
# 依赖: Java 21+, Maven, Node.js 18+, Redis

echo "🚀 DataForge Local Development Launcher"
echo "========================================"

# 1. 检查 Redis
echo ""
echo "📡 Checking Redis..."
if redis-cli ping >/dev/null 2>&1; then
    echo "   ✅ Redis is running"
else
    echo "   ⚠️  Redis not detected, trying to start..."
    if command -v redis-server >/dev/null 2>&1; then
        redis-server --daemonize yes
        sleep 1
        if redis-cli ping >/dev/null 2>&1; then
            echo "   ✅ Redis started successfully"
        else
            echo "   ❌ Failed to start Redis"
            exit 1
        fi
    else
        echo "   ❌ Redis is not installed."
        echo ""
        echo "   Install Redis:"
        echo "     macOS:  brew install redis && redis-server"
        echo "     Linux:  sudo apt-get install redis-server && sudo service redis-server start"
        echo "     Docker: docker run -d -p 6379:6379 redis:7-alpine"
        echo ""
        exit 1
    fi
fi

# 2. 启动后端
echo ""
echo "🔧 Starting Backend (Spring Boot + local profile)..."
cd "$(dirname "$0")/data-forge-web"
mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests &
BACKEND_PID=$!
cd "$(dirname "$0")"

# 等待后端启动
echo "   ⏳ Waiting for backend to start..."
for i in {1..60}; do
    if curl -s http://localhost:8080/api/v1/health >/dev/null 2>&1; then
        echo "   ✅ Backend ready at http://localhost:8080"
        break
    fi
    sleep 1
    if [ $i -eq 60 ]; then
        echo "   ⚠️  Backend startup timed out, check logs above"
    fi
done

# 3. 启动前端
echo ""
echo "🎨 Starting Frontend (Vite)..."
cd "$(dirname "$0")/data-forge-frontend"
npm run dev &
FRONTEND_PID=$!
cd "$(dirname "$0")"

echo ""
echo "========================================"
echo "✅ DataForge is starting up!"
echo ""
echo "   Frontend:     http://localhost:5173"
echo "   Backend API:  http://localhost:8080"
echo "   Swagger UI:   http://localhost:8080/swagger-ui.html"
echo "   H2 Console:   http://localhost:8080/h2-console"
echo "   Health Check: http://localhost:8080/api/v1/health"
echo ""
echo "   Backend PID:  $BACKEND_PID"
echo "   Frontend PID: $FRONTEND_PID"
echo ""
echo "   Press Ctrl+C to stop both services"
echo "========================================"

# 捕获退出信号，清理进程
trap 'echo ""; echo "🛑 Stopping services..."; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0' INT TERM

# 保持脚本运行
wait
