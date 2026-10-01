@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title 港湾跳蚤市场 - 一键启动

set ROOT=%~dp0
set ROOT=%ROOT:~0,-1%
set BACKEND_DIR=%ROOT%\后端\gwtzsc
set JAR=%BACKEND_DIR%\target\gwtzsc-0.0.1-SNAPSHOT.jar
set FRONTEND_DIR=%ROOT%\前端
set JAVA_HOME=D:\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo  ============================================
echo     港湾跳蚤市场 - 一键启动
echo  ============================================
echo.

:: ---------- 1. 检查 Java ----------
echo [检查] Java 21 ...
java -version 2>nul | findstr /c:"21" >nul
if errorlevel 1 (
    echo [错误] 未找到 JDK 21。请安装 JDK 21 或修改本文件中的 JAVA_HOME。
    pause
    exit /b 1
)
echo [OK]   Java 21 就绪

:: ---------- 2. 检查 Redis ----------
echo [检查] Redis ...
redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo [启动] 正在启动 Redis ...
    start /B redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru >nul 2>&1
    timeout /t 2 /nobreak >nul
    redis-cli ping >nul 2>&1
    if errorlevel 1 (
        echo [警告] Redis 未能启动（后端缓存功能可能受限，但不影响基本使用）
    ) else (
        echo [OK]   Redis 已启动
    )
) else (
    echo [OK]   Redis 已在运行
)

:: ---------- 3. 准备后端 JAR ----------
if not exist "%JAR%" (
    echo [编译] 未找到已编译的 jar，正在使用 Maven 编译（首次较慢）...
    cd /d "%BACKEND_DIR%"
    call mvnw.cmd clean package -DskipTests -q
    if errorlevel 1 (
        echo [错误] 后端编译失败，请检查上方日志。
        pause
        exit /b 1
    )
    cd /d "%ROOT%"
)
echo [OK]   后端程序就绪

:: ---------- 4. 检查后端是否已运行 ----------
powershell -Command "$c=New-Object Net.Sockets.TcpClient; try{$c.Connect('127.0.0.1',8080);$c.Close();exit 0}catch{exit 1}" >nul 2>&1
if not errorlevel 1 (
    echo [OK]   后端服务已在运行 (端口 8080)
    goto :frontend
)

:: ---------- 5. 启动后端（SQLite 模式） ----------
echo [启动] 后端服务 ...
start "港湾跳蚤市场-后端" /min cmd /c "java -jar \"%JAR%\" --spring.profiles.active=sqlite >\"%TEMP%\gwtzsc-backend.log\" 2>&1"
echo       后端日志: %TEMP%\gwtzsc-backend.log

echo [等待] 后端启动中 ...
set /a count=0
:wait_backend
powershell -Command "$c=New-Object Net.Sockets.TcpClient; try{$c.Connect('127.0.0.1',8080);$c.Close();exit 0}catch{exit 1}" >nul 2>&1
if errorlevel 1 (
    set /a count+=1
    if !count! geq 60 (
        echo [错误] 后端启动超时。请查看日志: %TEMP%\gwtzsc-backend.log
        pause
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    goto wait_backend
)
echo [OK]   后端服务已启动 (http://localhost:8080/api)

:: ---------- 6. 启动前端 ----------
:frontend
echo [检查] 前端服务 ...
powershell -Command "$c=New-Object Net.Sockets.TcpClient; try{$c.Connect('127.0.0.1',3000);$c.Close();exit 0}catch{exit 1}" >nul 2>&1
if errorlevel 1 (
    echo [启动] 前端服务 ...
    start "港湾跳蚤市场-前端" /B npx -y http-server -p 3000 -c-1 "%FRONTEND_DIR%" >"%TEMP%\gwtzsc-frontend.log" 2>&1
    timeout /t 4 /nobreak >nul
    echo [OK]   前端服务已启动 (http://localhost:3000)
) else (
    echo [OK]   前端服务已在运行
)

:: ---------- 7. 完成 ----------
echo.
echo  ============================================
echo      启动完成！
echo  ============================================
echo.
echo    用户前台:  http://localhost:3000
echo    管理后台:  http://localhost:3000/admin.html
echo    后端接口:  http://localhost:8080/api
echo.
echo    管理员账号: admin
echo    管理员密码: admin123
echo.
echo  ============================================
echo.

start "" "http://localhost:3000"

echo 按任意键关闭此窗口（服务将在后台继续运行）...
pause >nul
endlocal
