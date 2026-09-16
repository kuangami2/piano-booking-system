@echo off
setlocal
title Room Reservation - start local services

set "PROJ=E:\Pycharm_Python\room-reservation"
set "MVN=E:\env\apache-maven-3.8.1\bin\mvn.cmd"
set "NVM=E:\env\nvm\nvm.exe"
rem use the real nvm version directory, not E:\env\nodejs:
rem that path is a symlink and cmd's "if exist" does not follow it,
rem while other tools (PowerShell, WSL) do, which makes it unreliable here
set "NODE_DIR=E:\env\nvm\v24.3.0"
rem set JAVA_HOME here so the started windows inherit it; quoting it inside
rem cmd /k "..." breaks parsing because of the nested quotes
set "JAVA_HOME=E:\env\IntelliJ IDEA 2024.1.7\jbr"

echo ==========================================================
echo  Room Reservation - start local services
echo  backend 9090 / user-web 5173 / admin-web 5174
echo ==========================================================
echo.

if not exist "%PROJ%\backend\pom.xml" (
  echo [ERROR] project not found at %PROJ%
  echo.
  pause
  exit /b 1
)
if not exist "%MVN%" (
  echo [ERROR] maven not found at %MVN%
  echo.
  pause
  exit /b 1
)
if not exist "%NODE_DIR%\npm.cmd" (
  echo [ERROR] npm.cmd not found in %NODE_DIR%
  echo         check installed node versions under E:\env\nvm
  echo.
  pause
  exit /b 1
)

echo [0/3] switching node to 24.3.0 once for both front ends ...
"%NVM%" use 24.3.0

echo [1/3] backend on 9090 ...
start "room-backend" cmd /k "cd /d %PROJ%\backend&& call %MVN% spring-boot:run"

echo [2/3] user web on 5173 ...
start "room-user-web" cmd /k "cd /d %PROJ%\frontend\user-web&& call %NODE_DIR%\npm.cmd run dev"

echo [3/3] admin web on 5174 ...
start "room-admin-web" cmd /k "cd /d %PROJ%\frontend\admin-web&& call %NODE_DIR%\npm.cmd run dev"

echo.
echo waiting for ports, checking every 2 seconds, giving up after 90 seconds ...
set /a waited=0
:waitloop
timeout /t 2 /nobreak >nul
set /a waited+=2
set "ready=1"
netstat -ano | findstr ":9090" | findstr LISTENING >nul || set "ready="
netstat -ano | findstr ":5173" | findstr LISTENING >nul || set "ready="
netstat -ano | findstr ":5174" | findstr LISTENING >nul || set "ready="
if not defined ready if %waited% lss 90 goto waitloop

echo.
echo ----------------------------------------------------------
netstat -ano | findstr ":9090" | findstr LISTENING >nul
if errorlevel 1 (echo  backend  9090  NOT LISTENING - check the room-backend window) else (echo  backend  9090  OK)
netstat -ano | findstr ":5173" | findstr LISTENING >nul
if errorlevel 1 (echo  user web 5173  NOT LISTENING - check the room-user-web window) else (echo  user web 5173  OK)
netstat -ano | findstr ":5174" | findstr LISTENING >nul
if errorlevel 1 (echo  admin web 5174 NOT LISTENING - check the room-admin-web window) else (echo  admin web 5174 OK)

sc query RabbitMQ | findstr /C:"RUNNING" >nul
if errorlevel 1 (echo  RabbitMQ        NOT running - event delivery falls back to sync) else (echo  RabbitMQ        running)
echo  waited %waited% seconds
echo ----------------------------------------------------------
echo.
echo  user web   http://localhost:5173   account demo / 123456
echo  admin web  http://localhost:5174   account admin / admin123
echo  backend    http://127.0.0.1:9090
echo.
echo  if a front end says proxy ECONNREFUSED 9090, the backend is
echo  not up yet; wait a moment and refresh, or check its window.
echo.

start "" http://localhost:5173
start "" http://localhost:5174
pause
