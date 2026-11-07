@echo off
chcp 65001 >nul
title ShortBan PC — Otomatik Kurulum ve Guncelleyici (v4.1)
color 0C

echo ============================================================================
echo   SHORTBAN PC EKLENTISI - TEK TIKLA OTOMATIK KURULUM VE GUNCELLEME ARACI
echo   Gelistirici secenekleriyle ugrasmadan otomatik kurulum yapiliyor...
echo ============================================================================
echo.

set "INSTALL_DIR=%LOCALAPPDATA%\ShortBan"
set "EXT_DIR=%INSTALL_DIR%\Extension"
set "ZIP_FILE=%INSTALL_DIR%\ShortBan-Extension.zip"
set "UPDATER_CMD=%INSTALL_DIR%\ShortBan-AutoUpdate.cmd"
set "UPDATER_VBS=%INSTALL_DIR%\ShortBan-SilentUpdate.vbs"
set "ZIP_URL=https://raw.githubusercontent.com/sytex10/scrollblock/main/web/site/downloads/ScrollBlock-PC-Extension.zip"

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
if not exist "%EXT_DIR%" mkdir "%EXT_DIR%"

echo [1/4] En guncel ShortBan PC eklentisi GitHub uzerinden indiriliyor...
curl.exe -sL "%ZIP_URL%" -o "%ZIP_FILE%"
if exist "%ZIP_FILE%" (
    tar.exe -xf "%ZIP_FILE%" -C "%EXT_DIR%" 2>nul
)

echo [2/4] Arka plan otomatik guncelleme servisi kuruluyor...
(
echo @echo off
echo curl.exe -sL "%ZIP_URL%" -o "%ZIP_FILE%"
echo if exist "%ZIP_FILE%" tar.exe -xf "%ZIP_FILE%" -C "%EXT_DIR%" 2^>nul
) > "%UPDATER_CMD%"

(
echo Set WshShell = CreateObject^("WScript.Shell"^)
echo WshShell.Run chr^(34^) ^& "%UPDATER_CMD%" ^& chr^(34^), 0
echo Set WshShell = Nothing
) > "%UPDATER_VBS%"

schtasks /Create /SC HOURLY /MO 1 /TN "ShortBanAutoUpdater" /TR "wscript.exe \"%UPDATER_VBS%\"" /F >nul 2>&1

echo [3/4] Masaustu kisayolu ve tarayici entegrasyonu hazirlaniyor...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ext = '%EXT_DIR%'; " ^
  "$chrome = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe' -ErrorAction SilentlyContinue).'(default)'; " ^
  "if (-not $chrome) { $chrome = \"$env:ProgramFiles\Google\Chrome\Application\chrome.exe\" }; " ^
  "$edge = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\msedge.exe' -ErrorAction SilentlyContinue).'(default)'; " ^
  "if (-not $edge) { $edge = \"${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe\" }; " ^
  "$target = if (Test-Path $chrome) { $chrome } elseif (Test-Path $edge) { $edge } else { $null }; " ^
  "if ($target) { " ^
  "  $wsh = New-Object -ComObject WScript.Shell; " ^
  "  $lnk = $wsh.CreateShortcut(\"$HOME\Desktop\ShortBan Tarayici.lnk\"); " ^
  "  $lnk.TargetPath = $target; " ^
  "  $lnk.Arguments = \"--load-extension=`\"$ext`\" https://vsolken.com/shortban\"; " ^
  "  $lnk.Save(); " ^
  "}"

echo [4/4] Tarayiciniz ShortBan Korumasi aktif olarak baslatiliyor...
set "CHROME_EXE=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME_EXE%" set "CHROME_EXE=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME_EXE%" set "CHROME_EXE=%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"

set "EDGE_EXE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE_EXE%" set "EDGE_EXE=%ProgramFiles%\Microsoft\Edge\Application\msedge.exe"

if exist "%CHROME_EXE%" (
    taskkill /F /IM chrome.exe >nul 2>&1
    timeout /t 1 /nobreak >nul
    start "" "%CHROME_EXE%" --restore-last-session --load-extension="%EXT_DIR%" "https://vsolken.com/shortban"
) else if exist "%EDGE_EXE%" (
    taskkill /F /IM msedge.exe >nul 2>&1
    timeout /t 1 /nobreak >nul
    start "" "%EDGE_EXE%" --restore-last-session --load-extension="%EXT_DIR%" "https://vsolken.com/shortban"
)

echo.
echo ============================================================================
echo   TAMAMLANDI! ShortBan PC eklentisi kuruldu ve otomatik guncelleme aktif!
echo ============================================================================
timeout /t 3 >nul
exit

