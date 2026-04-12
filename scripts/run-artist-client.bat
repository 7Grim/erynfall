@echo off
setlocal

for %%I in ("%~dp0..") do set "REPO_ROOT=%%~fI"
set "JAR_PATH=%REPO_ROOT%\client\target\osrs-client-0.1.0-SNAPSHOT.jar"

echo [artist-launch] Repo root: %REPO_ROOT%
echo [artist-launch] Jar path:  %JAR_PATH%
echo [artist-launch] Mode:      artist

if not exist "%JAR_PATH%" (
  echo [artist-launch] ERROR: client jar not found.
  echo [artist-launch] Build it first:
  echo   mvn -pl client -am -DskipTests package
  exit /b 1
)

java -jar "%JAR_PATH%" --artist "--repo-root=%REPO_ROOT%"
