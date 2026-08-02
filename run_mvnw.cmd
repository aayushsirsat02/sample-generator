@echo off
setlocal
set "JAVA_HOME=C:\Progra~1\Eclipse\Adoptium\jdk-17.0.13.11-hotspot"
set "PATH=C:\Windows\System32;C:\Windows;C:\Windows\System32\WindowsPowerShell\v1.0"
cd /d "%~dp0"
call mvnw.cmd -q package -DskipTests
endlocal
