@ECHO OFF
SETLOCAL
PUSHD "%~dp0"
IF ERRORLEVEL 1 EXIT /B 1

SET "COMPOSE_CMD=docker compose -f docker\docker-compose.yml"
SET "RESULT=0"

%COMPOSE_CMD% build build
IF ERRORLEVEL 1 GOTO FAILED
%COMPOSE_CMD% run --rm --no-deps build
IF ERRORLEVEL 1 GOTO FAILED
ECHO Linux native executable created: %~dp0dist\talispam
GOTO END

:FAILED
SET "RESULT=1"

:END
POPD
ENDLOCAL & EXIT /B %RESULT%
