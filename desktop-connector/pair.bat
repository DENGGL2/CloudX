@echo off
setlocal
if "%~1"=="" goto defaultMode
start "" powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "%~dp0pair.ps1" %*
goto done
:defaultMode
start "" powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "%~dp0pair.ps1" -SelectedMode 1
:done
endlocal & exit /b 0
