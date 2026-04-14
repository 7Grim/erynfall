@echo off
setlocal enabledelayedexpansion

set "DIR=%~dp0"
set "JAR_PATH=%DIR%server\target\osrs-server-0.1.0-SNAPSHOT.jar"
set "ROOT_ENV_PATH=%DIR%.env.server.local"
set "SERVER_ENV_PATH=%DIR%server\.env.server.local"
set "WORLD_ID=%ERYNFALL_WORLD_ID%"
if "%WORLD_ID%"=="" set "WORLD_ID=sandbox"
set "JVM_ARGS="
set "APP_ARGS="

:parse_args
if "%~1"=="" goto after_args
if /I "%~1"=="--help" goto usage
if /I "%~1"=="-h" goto usage

if /I "%~1"=="--world" (
  if "%~2"=="" (
    echo [run-server] Missing value for --world
    goto usage_err
  )
  set "WORLD_ID=%~2"
  shift
  shift
  goto parse_args
)

set "ARG=%~1"
if /I "%ARG:~0,8%"=="--world=" (
  set "WORLD_ID=%ARG:~8%"
  shift
  goto parse_args
)

if /I "%ARG:~0,18%"=="-Derynfall.worldId" (
  for /f "tokens=1,* delims==" %%A in ("%ARG%") do set "WORLD_ID=%%B"
  shift
  goto parse_args
)

if "%ARG:~0,1%"=="-" (
  set "JVM_ARGS=%JVM_ARGS% %ARG%"
) else (
  set "APP_ARGS=%APP_ARGS% %ARG%"
)
shift
goto parse_args

:after_args
if /I not "%WORLD_ID%"=="sandbox" if /I not "%WORLD_ID%"=="main_world" (
  echo [run-server] Unsupported world for normal local launch: %WORLD_ID%
  goto usage_err
)

if not exist "%JAR_PATH%" (
  echo [run-server] Missing server jar: %JAR_PATH%
  echo [run-server] Build it with: mvn -pl shared,server -am -DskipTests package
  exit /b 1
)

set "LOCAL_ENV_PATH="
if exist "%SERVER_ENV_PATH%" (
  set "LOCAL_ENV_PATH=%SERVER_ENV_PATH%"
) else if exist "%ROOT_ENV_PATH%" (
  set "LOCAL_ENV_PATH=%ROOT_ENV_PATH%"
)

if not "%LOCAL_ENV_PATH%"=="" (
  echo [run-server] Loading local env: %LOCAL_ENV_PATH%
  for /f "usebackq tokens=* delims=" %%L in ("%LOCAL_ENV_PATH%") do call :load_env_line "%%L"
)

echo [run-server] World: %WORLD_ID%
echo [run-server] Jar:   %JAR_PATH%

if "%DB_PASSWORD%"=="" echo [run-server] WARN: DB_PASSWORD is not set. Azure DB login will fail and the server will fall back to in-memory mode.
if "%JWT_SIGNING_KEY%"=="" echo [run-server] WARN: JWT_SIGNING_KEY is not set. Token-auth login from the Azure auth service will be rejected by the local game server.
if "%JWT_ISSUER%"=="" echo [run-server] WARN: JWT_ISSUER is not set. Token verification may fail even if JWT_SIGNING_KEY is present.
if "%JWT_AUDIENCE%"=="" echo [run-server] WARN: JWT_AUDIENCE is not set. Token verification may fail even if JWT_SIGNING_KEY is present.

java %JVM_ARGS% -Derynfall.worldId=%WORLD_ID% -jar "%JAR_PATH%" %APP_ARGS%
exit /b %errorlevel%

:load_env_line
set "LINE=%~1"
if "%LINE%"=="" goto :eof
if "%LINE:~0,1%"=="#" goto :eof
for /f "tokens=1,* delims==" %%A in ("%LINE%") do set "%%A=%%B"
goto :eof

:usage
echo Usage: run-server.bat [--world ^<worldId^>] [jvm args]
echo.
echo World options:
echo   sandbox     Local/dev test world ^(default^)
echo   main_world  Main live-world scaffold
echo.
echo Examples:
echo   run-server.bat
echo   run-server.bat --world sandbox
echo   run-server.bat --world main_world
echo   run-server.bat -Derynfall.worldId=main_world
echo.
echo The launcher also reads local env from:
echo   server\.env.server.local
echo   .env.server.local
exit /b 0

:usage_err
call :usage
exit /b 1
