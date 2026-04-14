@echo off
setlocal

set "DIR=%~dp0"
set "JAR_PATH=%DIR%client\target\osrs-client-0.1.0-SNAPSHOT.jar"
set "SERVER_HOST=%GAME_SERVER_HOST%"
if "%SERVER_HOST%"=="" set "SERVER_HOST=localhost"

if not exist "%JAR_PATH%" (
  echo [run-client] Missing client jar: %JAR_PATH%
  echo [run-client] Build it with: mvn -pl shared,client -am -DskipTests package
  exit /b 1
)

echo [run-client] Host: %SERVER_HOST%
echo [run-client] Jar:  %JAR_PATH%

java -DGAME_SERVER_HOST=%SERVER_HOST% -jar "%JAR_PATH%" %*
