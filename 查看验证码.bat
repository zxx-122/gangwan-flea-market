@echo off
setlocal enabledelayedexpansion
title 查看短信验证码
mode con cols=72 lines=24

echo.
echo  ============================================
echo        查询当前短信验证码（Redis）
echo  ============================================
echo.

set FOUND=0
for /f "usebackq delims=" %%k in (`redis-cli keys "sms:code:*" 2^>nul`) do (
    set FOUND=1
    set PHONE=%%k
    set PHONE=!PHONE:sms:code:=!
    for /f "usebackq delims=" %%c in (`redis-cli GET "%%k" 2^>nul`) do set CODE=%%c
    for /f "usebackq delims=" %%t in (`redis-cli TTL "%%k" 2^>nul`) do set TTL=%%t
    echo.
    echo   手机号   : !PHONE!
    echo   验证码   : !CODE!
    echo   剩余有效 : !TTL! 秒（5 分钟内有效）
    echo   ----------------------------------------
)

if "!FOUND!"=="0" (
    echo   [提示] Redis 中当前没有任何验证码。
    echo.
    echo   请先在前端页面点击「发送验证码」按钮，
    echo   5 分钟内再运行本工具即可看到。
)

echo.
echo  ============================================
echo.
pause
endlocal
