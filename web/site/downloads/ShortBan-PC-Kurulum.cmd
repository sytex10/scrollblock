@echo off
chcp 65001 >nul
title ShortBan PC — Otomatik Kurulum ve Guncelleyici (v4.4)

set "INSTALL_DIR=%LOCALAPPDATA%\ShortBan"
set "EXT_DIR=%INSTALL_DIR%\Extension"
set "DL_EXT_DIR=%USERPROFILE%\Downloads\ScrollBlock-PC-Extension"
set "ZIP_FILE=%INSTALL_DIR%\ShortBan-Extension.zip"
set "UPDATER_CMD=%INSTALL_DIR%\ShortBan-AutoUpdate.cmd"
set "UPDATER_VBS=%INSTALL_DIR%\ShortBan-SilentUpdate.vbs"
set "ZIP_URL=https://raw.githubusercontent.com/sytex10/scrollblock/main/web/site/downloads/ScrollBlock-PC-Extension.zip"

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
if not exist "%EXT_DIR%" mkdir "%EXT_DIR%"
if not exist "%DL_EXT_DIR%" mkdir "%DL_EXT_DIR%"

curl.exe -sL "%ZIP_URL%" -o "%ZIP_FILE%"
if exist "%ZIP_FILE%" (
    tar.exe -xf "%ZIP_FILE%" -C "%EXT_DIR%" 2>nul
    tar.exe -xf "%ZIP_FILE%" -C "%DL_EXT_DIR%" 2>nul
)

(
echo @echo off
echo curl.exe -sL "%ZIP_URL%" -o "%ZIP_FILE%"
echo if exist "%ZIP_FILE%" ^(
echo     tar.exe -xf "%ZIP_FILE%" -C "%EXT_DIR%" 2^>nul
echo     if exist "%DL_EXT_DIR%" tar.exe -xf "%ZIP_FILE%" -C "%DL_EXT_DIR%" 2^>nul
echo ^)
) > "%UPDATER_CMD%"

(
echo Set WshShell = CreateObject^("WScript.Shell"^)
echo WshShell.Run chr^(34^) ^& "%UPDATER_CMD%" ^& chr^(34^), 0
echo Set WshShell = Nothing
) > "%UPDATER_VBS%"

schtasks /Create /SC MINUTE /MO 15 /TN "ShortBanAutoUpdater" /TR "wscript.exe \"%UPDATER_VBS%\"" /F >nul 2>&1
exit
