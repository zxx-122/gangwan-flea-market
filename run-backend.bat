@echo off
chcp 65001 >nul
title 港湾跳蚤市场 - 后端服务

echo ================================
echo   港湾跳蚤市场 - 后端启动
echo ================================
echo.

cd /d "%~dp0后端\gwtzsc"

:: 设置 Java 环境
set JAVA_HOME=D:\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%

:: 检�?Java
java -version 2>&1 | findstr /c:"version \"2" >nul
if %errorlevel% neq 0 (
    echo [错误] 请安�?JDK 21+
    pause
    exit /b 1
)
echo [OK] Java 21

:: 启动 Redis（如果未运行�?redis-cli ping >nul 2>&1
if %errorlevel% neq 0 (
    echo [*] 启动 Redis...
    start /B redis-server --maxmemory 128mb --maxmemory-policy allkeys-lru >nul 2>&1
    timeout /t 2 /nobreak >nul
)
echo [OK] Redis

:: 选择数据�?echo.
echo 请选择数据库：
echo   1. MySQL
echo   2. SQLite（默认）
set /p DB="输入数字 (1/2): "
if "%DB%"=="1" (
    set PROFILE=mysql
) else (
    set PROFILE=sqlite
)
echo [OK] 使用 %PROFILE%

:: 启动后端
echo.
echo [*] 启动后端...
echo     端口: 8080
echo     访问: http://localhost:8080/api
echo.
echo �?Ctrl+C 停止服务
echo ================================
echo.

set "MAVEN_OPTS=-Xmx256m"
 call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=%PROFILE% -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m"
