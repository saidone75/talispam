@ECHO OFF
SETLOCAL
PUSHD "%~dp0"
IF ERRORLEVEL 1 EXIT /B 1

SET "COMPOSE_CMD=docker compose -f docker\docker-compose.yml"
SET "RESULT=0"

IF /I "%~1"=="build" GOTO BUILD
IF /I "%~1"=="purge" GOTO PURGE
ECHO Usage: %~nx0 {build^|purge}
SET "RESULT=1"
GOTO END

:BUILD
%COMPOSE_CMD% build build
IF ERRORLEVEL 1 GOTO FAILED
%COMPOSE_CMD% run --rm --no-deps build
IF ERRORLEVEL 1 GOTO FAILED
ECHO Linux native executable created: %~dp0dist\talispam
GOTO END

:PURGE
%COMPOSE_CMD% down --rmi local --remove-orphans
IF ERRORLEVEL 1 GOTO FAILED
GOTO END

:FAILED
SET "RESULT=1"

:END
POPD
ENDLOCAL & EXIT /B %RESULT%
