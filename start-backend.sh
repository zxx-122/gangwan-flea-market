#!/bin/bash
# 港湾跳蚤市场 - 后端一键启动

cd "$(dirname "$0")/后端/gwtzsc"

export JAVA_HOME=/d/jdk21
export PATH=$JAVA_HOME/bin:$PATH

echo "================================"
echo "  后端启动中..."
echo "================================"

# 启动 Redis（如果未运行）
/c/Users/AAA/redis/redis-cli.exe ping > /dev/null 2>&1 || {
    nohup /c/Users/AAA/redis/redis-server.exe --maxmemory 128mb --maxmemory-policy allkeys-lru > /tmp/redis.log 2>&1 &
    sleep 1
}

echo "后端地址: http://localhost:8080/api"
echo ""
echo "按 Ctrl+C 停止"
echo "================================"

export MAVEN_OPTS="-Xmx256m"
 ./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=sqlite -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"