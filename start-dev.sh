#!/usr/bin/env bash
# ========================================================
# 港湾跳蚤市场 — 开发环境一键启动 (Git Bash / MSYS2)
# 用法: bash start-dev.sh
# ========================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/前端"
BACKEND_DIR="$SCRIPT_DIR/后端/gwtzsc"
UPLOAD_DIR="$SCRIPT_DIR/uploads"

# ---------- 1. 环境配置 ----------
export JAVA_HOME="${JAVA_HOME:-/d/jdk21}"
export PATH="$JAVA_HOME/bin:$PATH"
export REDIS_HOME="${REDIS_HOME:-/c/Users/AAA/redis}"

echo "================================"
echo "  港湾跳蚤市场 — 一键启动"
echo "================================"
echo ""

# ---------- 2. 检查 Java ----------
if ! java -version 2>&1 | grep -q 'version "2[1-9]'; then
  echo "[错误] 需要 JDK 21+，请设置 JAVA_HOME"
  echo "       当前 JAVA_HOME: $JAVA_HOME"
  exit 1
fi
echo "[OK] Java: $(java -version 2>&1 | head -1)"

# ---------- 3. 检查 Node ----------
if ! node -v &>/dev/null; then
  echo "[错误] 需要 Node.js，请安装"
  exit 1
fi
echo "[OK] Node.js: $(node -v)"

# ---------- 4. 启动 Redis ----------
echo "[*] 检查 Redis..."
if "$REDIS_HOME/redis-cli.exe" ping 2>/dev/null | grep -q PONG; then
  echo "[OK] Redis 已运行"
else
  if [ -x "$REDIS_HOME/redis-server.exe" ]; then
    nohup "$REDIS_HOME/redis-server.exe" > /tmp/gwtzsc-redis.log 2>&1 &
    sleep 2
    if "$REDIS_HOME/redis-cli.exe" ping 2>/dev/null | grep -q PONG; then
      echo "[OK] Redis 已启动"
    else
      echo "[警告] Redis 启动失败，后端无法使用缓存"
    fi
  else
    echo "[警告] 找不到 redis-server，后端无法使用缓存"
  fi
fi

# ---------- 5. 创建上传目录 ----------
mkdir -p "$UPLOAD_DIR"

# ---------- 6. 选择数据库 ----------
echo ""
echo "请选择数据库模式："
echo "  1. MySQL  (需提前启动 MySQL 服务)"
echo "  2. SQLite (文件数据库，无需安装)"
DB_CHOICE=""
while [ -z "$DB_CHOICE" ]; do
  read -rp "输入数字 (1/2) [默认2]: " input
  DB_CHOICE="${input:-2}"
  if [ "$DB_CHOICE" != "1" ] && [ "$DB_CHOICE" != "2" ]; then
    echo "[错误] 请输入 1 或 2"
    DB_CHOICE=""
  fi
done

if [ "$DB_CHOICE" == "1" ]; then
  DB_PROFILE="mysql"
else
  DB_PROFILE="sqlite"
fi
echo "[OK] 使用数据库: $DB_PROFILE"

# ---------- 7. 启动后端 ----------
echo ""
echo "[*] 启动后端 (Profile: $DB_PROFILE)..."
cd "$BACKEND_DIR"
MVNW="./mvnw"
[ ! -f "$MVNW" ] && MVNW="mvn"

nohup $MVNW spring-boot:run -Dspring-boot.run.profiles="$DB_PROFILE" -q > /tmp/gwtzsc-backend.log 2>&1 &
BACKEND_PID=$!

echo "  后端 PID: $BACKEND_PID"
echo "  等待后端就绪..."

BACKEND_READY=0
for i in $(seq 1 90); do
  if bash -c 'exec 3<>/dev/tcp/localhost/8080' 2>/dev/null; then
    # 测试 API 是否可用
    if curl -s http://localhost:8080/api/category/list > /dev/null 2>&1; then
      echo "  [OK] 后端已就绪 (端口 8080)"
      BACKEND_READY=1
      break
    fi
  fi
  sleep 2
done

if [ "$BACKEND_READY" -eq 0 ]; then
  echo "  [错误] 后端启动超时，请检查 /tmp/gwtzsc-backend.log"
  echo "  日志最后几行:"
  tail -20 /tmp/gwtzsc-backend.log
  exit 1
fi

cd "$SCRIPT_DIR"

# ---------- 8. 启动前端 ----------
echo ""
echo "[*] 启动前端服务器..."
nohup npx -y http-server "$FRONTEND_DIR" -p 3000 > /tmp/gwtzsc-frontend.log 2>&1 &
FRONTEND_PID=$!
sleep 3

# 验证前端
echo "  验证前端服务..."
if curl -s http://localhost:3000 > /dev/null 2>&1; then
  echo "  [OK] 前端已就绪 (端口 3000)"
else
  echo "  [警告] 前端可能未正常启动"
fi

# ---------- 9. 输出信息 ----------
echo ""
echo "================================"
echo "  ✅ 启动完成！"
echo "================================"
echo ""
echo "  数据库模式: $DB_PROFILE"
echo "  前端地址:   http://localhost:3000"
echo "  管理后台:   http://localhost:3000/admin.html"
echo "  后端接口:   http://localhost:8080/api"
echo ""
echo "  默认管理员: admin / Admin123"
echo ""
echo "  停止服务:"
echo "    后端: kill $BACKEND_PID"
echo "    前端: kill $FRONTEND_PID"
echo "    Redis: $REDIS_HOME/redis-cli.exe shutdown"
echo ""
echo "  停止全部: kill $BACKEND_PID $FRONTEND_PID"
echo ""
echo "  日志文件:"
echo "    后端: /tmp/gwtzsc-backend.log"
echo "    前端: /tmp/gwtzsc-frontend.log"
echo "    Redis: /tmp/gwtzsc-redis.log"
