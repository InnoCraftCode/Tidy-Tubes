@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem ---------------------------------------------------------------------------
rem  Supprime les restes du projet Android Studio d'origine, qui empechent
rem  Gradle de demarrer (settings.gradle et settings.gradle.kts en conflit).
rem  Ne touche a rien du projet libGDX : core, lwjgl3, android, gradlew,
rem  gradle/wrapper, gradle/libs.versions.toml, local.properties, README.
rem
rem  Ferme Android Studio avant de lancer : un IDE ouvert verrouille .idea et
rem  .gradle, et la suppression echoue silencieusement sur ces deux dossiers.
rem ---------------------------------------------------------------------------

if not exist "settings.gradle.kts" (
    echo.
    echo   [!] settings.gradle.kts introuvable.
    echo       Ce script doit etre lance depuis la racine du projet.
    echo.
    pause
    exit /b 1
)

echo.
echo   Nettoyage de E:\Sortpuzzle
echo   --------------------------------------------------
call :killdir  "app"
call :killdir  "build"
call :killdir  ".gradle"
call :killdir  ".idea"
call :killfile "build.gradle"
call :killfile "settings.gradle"
call :killfile "gradle\gradle-daemon-jvm.properties"
echo   --------------------------------------------------
echo.
echo   Termine. Ensuite :
echo       gradlew :core:test
echo       gradlew :lwjgl3:run
echo.
echo   Ce script se supprime lui-meme en quittant.
echo.
pause

rem Sort du contexte batch avant d'effacer le fichier, sinon cmd se plaint
rem de ne plus trouver la ligne suivante.
(goto) 2>nul & del /f /q "%~f0"

:killdir
if exist "%~1\" (
    rd /s /q "%~1" 2>nul
    if exist "%~1\" (
        echo     VERROUILLE  %~1\     ^(ferme Android Studio et relance^)
    ) else (
        echo     supprime    %~1\
    )
) else (
    echo     deja absent %~1\
)
exit /b 0

:killfile
if exist "%~1" (
    del /f /q "%~1" 2>nul
    if exist "%~1" (
        echo     VERROUILLE  %~1
    ) else (
        echo     supprime    %~1
    )
) else (
    echo     deja absent %~1
)
exit /b 0
