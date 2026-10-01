#!/usr/bin/env bash
# ========================================================
# 港湾跳蚤市场 -- 一键启动脚本 (Git Bash / Linux)
# 用法: bash start.sh
# ========================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/前端"
BACKEND_DIR="$SCRIPT_DIR/后端/gwtzsc"
UPLOAD_DIR="$SCRIPT_DIR/uploads"

echo "================================"
echo "  港湾跳蚤市场 -- 一键启动"
echo "================================"

# ---------- 选择数据库 ----------
while true; do
  echo ""
  echo "请选择数据库："
  echo "  1. MySQL  (需提前启动 MySQL 服务)"
  echo "  2. SQLite (文件数据库，无需额外安装)"
  read -rp "输入数字 (1/2): " DB_CHOICE
  case "$DB_CHOICE" in
    1) DB_PROFILE="mysql"; break ;;
    2) DB_PROFILE="sqlite"; break ;;
    *) echo "[错误] 无效选择，请输入 1 或 2" ;;
  esac
done

# ---------- 1. 检查 Java ----------
if ! java -version 2>&1 | grep -q 'version "2[1-9]'; then
  echo "[错误] 需要 JDK 21+，请安装并配置 JAVA_HOME"
  exit 1
fi
echo "[OK] Java"

# ---------- 2. 检查 Node ----------
if ! node -v &>/dev/null; then
  echo "[错误] 需要 Node.js，请安装"
  exit 1
fi
echo "[OK] Node.js"

# ---------- 3. 启动 Redis ----------
echo "[*] 检查 Redis..."
if redis-cli ping 2>/dev/null | grep -q PONG; then
  echo "[OK] Redis 已运行"
else
  REDIS_BIN=$(which redis-server 2>/dev/null || echo "$HOME/redis/redis-server")
  if [ -x "$REDIS_BIN" ]; then
    nohup "$REDIS_BIN" --maxmemory 128mb --maxmemory-policy allkeys-lru > /tmp/gwtzsc-redis.log 2>&1 &
    sleep 2
    if redis-cli ping 2>/dev/null | grep -q PONG; then
      echo "[OK] Redis 已启动"
    else
      echo "[警告] Redis 启动失败，后端无法正常工作"
    fi
  else
    echo "[警告] 找不到 redis-server，后端将无法缓存/验证码"
  fi
fi

# ---------- 4. 创建上传目录 ----------
mkdir -p "$UPLOAD_DIR"

# ---------- 5. SQLite 初始化 ----------
if [ "$DB_PROFILE" == "sqlite" ]; then
  echo "[*] SQLite 模式：检查数据库..."
  if [ ! -f "$SCRIPT_DIR/gwtzsc.db" ]; then
    echo "[*] 首次运行 SQLite，数据库文件将在后端启动时自动创建"
  else
    echo "[OK] SQLite 数据库已存在"
  fi
fi

# ---------- 6. 启动后端 ----------
echo "[*] 启动后端（Profile: $DB_PROFILE）..."
cd "$BACKEND_DIR"
MVNW="./mvnw"
[ ! -f "$MVNW" ] && MVNW="mvn"

export MAVEN_OPTS="-Xmx256m"
 nohup $MVNW spring-boot:run -Dspring-boot.run.profiles="$DB_PROFILE" -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m" -q > /tmp/gwtzsc-backend.log 2>&1 &
BACKEND_PID=$!

echo "  后端启动中 (PID $BACKEND_PID)..."
for i in $(seq 1 90); do
  if bash -c 'exec 3<>/dev/tcp/localhost/8080' 2>/dev/null; then
    echo "[OK] 后端已启动 (端口 8080, Profile: $DB_PROFILE)"
    break
  fi
  sleep 2
done
if [ "$i" -ge 90 ]; then
  echo "[警告] 后端启动超时，请查看 /tmp/gwtzsc-backend.log"
fi

cd "$SCRIPT_DIR"

# ---------- 7. 启动前端 ----------
echo "[*] 启动前端服务器..."
nohup npx -y http-server "$FRONTEND_DIR" -p 3000 > /tmp/gwtzsc-frontend.log 2>&1 &
FRONTEND_PID=$!
sleep 3
echo "[OK] 前端服务器已启动 (端口 3000)"

# ---------- 8. 输出信息 ----------
echo ""
echo "================================"
echo "  启动完成！"
echo "================================"
echo ""
echo "  数据库模式: $DB_PROFILE"
echo "  前端地址:   http://localhost:3000"
echo "  管理后台:   http://localhost:3000/admin.html"
echo "  后端接口:   http://localhost:8080"
echo ""
echo "  默认管理员: admin / Admin123"
echo ""
echo "  停止服务:"
echo "    后端: kill $BACKEND_PID"
echo "    前端: kill $FRONTEND_PID"
echo "    Redis: redis-cli shutdown"
echo ""
echo "  停止全部: kill $BACKEND_PID $FRONTEND_PID; redis-cli shutdown"
