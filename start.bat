@echo off
chcp 65001 >nul
title 港湾跳蚤市场 -- 一键启�?setlocal enabledelayedexpansion

echo ================================
echo   港湾跳蚤市场 -- 一键启�?echo ================================
echo.

set ROOT_DIR=%~dp0
set FRONTEND_DIR=%ROOT_DIR%前端
set BACKEND_DIR=%ROOT_DIR%后端\gwtzsc
set UPLOAD_DIR=%ROOT_DIR%uploads

:: ---------- 选择数据�?----------
:CHOICE_DB
echo 请选择数据库：
echo   1. MySQL   ^(需提前启动 MySQL 服务^)
echo   2. SQLite  ^(文件数据库，无需额外安装^)
set /p DB_CHOICE="输入数字 (1/2): "

if /I "%DB_CHOICE%"=="1" (
    set DB_PROFILE=mysql
) else if /I "%DB_CHOICE%"=="2" (
    set DB_PROFILE=sqlite
) else (
    echo [错误] 无效选择，请输入 1 �?2
goto :CHOICE_DB
)

:: ---------- 1. 检�?Java ----------
java -version 2>&1 | findstr /c:"version \"2" >nul
if %errorlevel% neq 0 (
    echo [错误] 需�?JDK 21+，请安装并配�?JAVA_HOME
    pause
    exit /b 1
)
echo [OK] Java

:: ---------- 2. 检�?Node ----------
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 需�?Node.js，请安装
    pause
    exit /b 1
)
echo [OK] Node.js

:: ---------- 3. 启动 Redis ----------
echo [*] 检�?Redis...
redis-cli ping >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Redis 已运�?) else (
    where redis-server >nul 2>&1
    if !errorlevel! equ 0 (
        start /B redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru > "%TEMP%\gwtzsc-redis.log" 2>&1
        timeout /t 2 /nobreak >nul
        redis-cli ping >nul 2>&1
        if !errorlevel! equ 0 (
            echo [OK] Redis 已启�?        ) else (
            echo [警告] Redis 启动失败
        )
    ) else (
        echo [警告] 找不�?redis-server，后端将无法缓存/验证�?    )
)

:: ---------- 4. 创建上传目录 ----------
if not exist "%UPLOAD_DIR%" mkdir "%UPLOAD_DIR%"

:: ---------- 5. SQLite 初始�?----------
if /I "%DB_PROFILE%"=="sqlite" (
    echo [*] SQLite 模式：检查数据库...
    if not exist "%ROOT_DIR%\gwtzsc.db" (
        echo [*] 首次运行 SQLite，正在初始化数据�?..
        if exist "%ROOT_DIR%\sql\init-sqlite.sql" (
            echo [提示] 请先安装 SQLite 命令行工具，或在后端启动时自动建�?            echo        数据库文件将在首次启动后自动生成
        )
    ) else (
        echo [OK] SQLite 数据库已存在
    )
)

:: ---------- 6. 启动后端 ----------
echo [*] 启动后端（Profile: %DB_PROFILE%�?..
cd /d "%BACKEND_DIR%"
set MVNW=mvnw.cmd
if not exist "%MVNW%" set MVNW=mvn

set "MAVEN_OPTS=-Xmx256m"
 start "gwtzsc-backend" /B %MVNW% spring-boot:run -Dspring-boot.run.profiles=%DB_PROFILE% -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m" -q > "%TEMP%\gwtzsc-backend.log" 2>&1
cd /d "%ROOT_DIR%"

:: 等待后端就绪
echo [*] 等待后端启动...
set BACKEND_READY=0
for /l %%i in (1,1,90) do (
    timeout /t 2 /nobreak >nul
    powershell -Command "& {try {$tcp=New-Object Net.Sockets.TcpClient; $tcp.Connect('127.0.0.1',8080); $tcp.Close(); exit 0} catch {exit 1}}" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [OK] 后端已启�?(端口 8080, Profile: %DB_PROFILE%)
        set BACKEND_READY=1
        goto :BACKEND_OK
    )
)
if "!BACKEND_READY!"=="0" (
    echo [警告] 后端启动超时，请查看 %%TEMP%%\gwtzsc-backend.log
)
:BACKEND_OK

:: ---------- 7. 启动前端 ----------
echo [*] 启动前端服务�?..
cd /d "%FRONTEND_DIR%"
start "gwtzsc-frontend" /B npx -y http-server -p 3000 > "%TEMP%\gwtzsc-frontend.log" 2>&1
timeout /t 3 /nobreak >nul
echo [OK] 前端服务器已启动 (端口 3000)

:: ---------- 8. 输出信息 ----------
echo.
echo ================================
echo   启动完成�?echo ================================
echo.
echo   数据库模�? %DB_PROFILE%
echo   前端地址:   http://localhost:3000
echo   管理后台:   http://localhost:3000/admin.html
echo   后端接口:   http://localhost:8080
echo.
echo   默认管理�? admin / Admin123
echo.
echo   提示：按任意键打开浏览器，关闭本窗口会同时关闭服务
echo.

start http://localhost:3000

pause
