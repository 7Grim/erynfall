@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM
@REM This file is based on the maven-wrapper distribution
@REM https://maven.apache.org/wrapper/
@REM
@REM This batch script will download Maven as needed and run it.
@REM
@REM Required ENV vars (with default paths if not set):
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars:
@REM M2_HOME - location of maven2 to use (default searches %PATH%)
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a key stroke before ending
@REM
@REM ============================================================================

@echo off

setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
setlocal

set MAVEN_CMD_LINE_ARGS=%*

%MAVEN_CMD_LINE_ARGS:?/=-%

if not "%MAVEN_CMD_LINE_ARGS%" == "" (
  for /f "tokens=1,* delims= " %%A in ("%MAVEN_CMD_LINE_ARGS%") do (
    call :action_set "%%A" "%%B"
  )
)

if "%DEBUG%" == "on" (
  @echo on
)

@REM Set the location of the Maven wrapper jar.
set MAVEN_WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar

@REM Provide a "standardized" way to retrieve the CLI args that will
@REM work with both Windows and non-Windows executions.
if ERRORLEVEL 1 (
  echo Error: JAVA_HOME not found in your environment. >&2
  echo Please set the JAVA_HOME variable in your environment to match the >&2
  echo location of your Java installation. >&2
  goto error
)

@REM For old Windows versions, check for javaw.exe.
if not exist "%JAVA_EXE%" (
  if exist "%JAVA_HOME%\bin\javaw.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\javaw.exe"
  )
)

if not exist "%JAVA_EXE%" (
  echo.
  echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
  echo.
  echo Please set the JAVA_HOME variable in your environment to match the
  echo location of your Java installation.
  echo.
  goto error
)

@REM see https://issues.apache.org/jira/browse/WAGON-519
@REM Set MAVEN_CMD_LINE_ARGS from the command line args
set "MAVEN_CMD_LINE_ARGS=%*"

@REM Run Maven
"%JAVA_EXE%" ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%CD%" ^
  org.apache.maven.wrapper.MavenWrapperMain %MAVEN_CMD_LINE_ARGS%
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%FINISH_CMD%"=="" (
  %FINISH_CMD%
)

exit /b %ERROR_CODE%

:action_set
goto :eof
