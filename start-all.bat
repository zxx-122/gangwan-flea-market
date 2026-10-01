@echo off
chcp 65001 >nul
title 港湾跳蚤市场 - 一键启动

echo.
echo  ================================
echo    港湾跳蚤市场 - 一键启动
echo  ================================
echo.

:: 设置 Java 环境
set JAVA_HOME=D:\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%

:: 检查 Java
echo [检查] Java 21...
java -version 2>nul | findstr "21" >nul
if errorlevel 1 (
    echo [错误] 请安装 JDK 21 或检查 JAVA_HOME 配置
    pause
    exit /b 1
)
echo [OK] Java 21 已就绪

:: 检查并启动 Redis
echo [检查] Redis...
redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo [启动] Redis 服务...
    start /B redis-server >nul 2>&1
    ping 127.0.0.1 -n 3 >nul
    redis-cli ping >nul 2>&1
    if errorlevel 1 (
        echo [错误] Redis 启动失败，请手动启动 Redis
        pause
        exit /b 1
    )
)
echo [OK] Redis 已就绪

:: 检查后端是否已运行
echo [检查] 后端服务...
curl -s http://localhost:8080/api/category/list >nul 2>&1
if not errorlevel 1 (
    echo [OK] 后端服务已运行
    goto :start_frontend
)

:: 启动后端
echo [启动] 后端服务...
cd /d "%~dp0后端\gwtzsc"
if not exist target\gwtzsc-0.0.1-SNAPSHOT.jar (
    echo [编译] 首次运行，正在编译...
    call mvnw.cmd clean package -DskipTests -q
    if errorlevel 1 (
        echo [错误] 编译失败
        pause
        exit /b 1
    )
)

start "港湾跳蚤市场-后端" cmd /c "set JAVA_HOME=D:\jdk21 && java -jar target\gwtzsc-0.0.1-SNAPSHOT.jar --spring.profiles.active=sqlite"

:: 等待后端启动
echo [等待] 后端服务启动中...
set /a count=0
:wait_backend
curl -s http://localhost:8080/api/category/list >nul 2>&1
if errorlevel 1 (
    set /a count+=1
    if !count! geq 30 (
        echo [错误] 后端启动超时
        pause
        exit /b 1
    )
    ping 127.0.0.1 -n 2 >nul
    goto wait_backend
)
echo [OK] 后端服务已启动

:start_frontend
cd /d "%~dp0"
echo.
echo  ================================
echo    服务启动完成！
echo  ================================
echo.
echo    后端地址: http://localhost:8080/api
echo    前端地址: %~dp0前端\index.html
echo    管理后台: %~dp0前端\admin.html
echo.
echo    管理员账号: admin
echo    管理员密码: admin123
echo.
echo  ================================
echo.

:: 打开前端页面
start "" "%~dp0前端\index.html"

echo 按任意键退出此窗口（服务将继续运行）...
pause >nul
