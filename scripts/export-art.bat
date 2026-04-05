@echo off
REM export-art.bat — Exports all Aseprite source files to art\sprites\
REM
REM Usage:
REM   scripts\export-art.bat              REM export everything in art\aseprite\
REM   scripts\export-art.bat player.ase   REM export a single file
REM
REM Requires Aseprite to be installed. Set ASEPRITE_PATH below if not on PATH.
REM
REM After running: mvn generate-resources -pl client -am
REM Then start the client (or press F5 in-game) to see your sprites.

SET ASEPRITE_PATH=aseprite
SET ASE_DIR=%~dp0..\art\aseprite
SET OUT_DIR=%~dp0..\art\sprites

IF NOT EXIST "%OUT_DIR%" MKDIR "%OUT_DIR%"

IF "%~1"=="" (
    REM Export all files
    FOR %%f IN ("%ASE_DIR%\*.aseprite" "%ASE_DIR%\*.ase") DO (
        IF EXIST "%%f" CALL :ExportFile "%%f"
    )
) ELSE (
    CALL :ExportFile "%~1"
)

ECHO.
ECHO Done. Now run: mvn generate-resources -pl client -am
ECHO Then start the client (or press F5 in-game) to see your sprites.
GOTO :EOF

:ExportFile
SET ASE=%~1
SET BASE=%~n1
REM Export with tag-split for animations
"%ASEPRITE_PATH%" -b --split-tags --filename-format "{title}_{tag}_{tagframe}.png" "%ASE%" --save-as "%OUT_DIR%\{title}_{tag}_{tagframe}.png"
IF ERRORLEVEL 1 (
    REM No tags — export as static sprite
    "%ASEPRITE_PATH%" -b "%ASE%" --save-as "%OUT_DIR%\%BASE%.png"
    ECHO   Exported static: %BASE%
) ELSE (
    ECHO   Exported animated: %BASE%
)
GOTO :EOF
