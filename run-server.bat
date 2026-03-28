@echo off
set DIR=%~dp0
java -jar "%DIR%server\target\osrs-server-0.1.0-SNAPSHOT.jar" %*
