@ECHO OFF
SETLOCAL
SET "BUILD_OPTIONS="

:PARSE_ARGS
IF "%~1"=="" GOTO BUILD
IF "%~1"=="--no-cache" (
    SET "BUILD_OPTIONS=--no-cache"
    SHIFT
    GOTO PARSE_ARGS
)
IF "%~1"=="--help" GOTO HELP
IF "%~1"=="-h" GOTO HELP
ECHO Unknown option: %~1 1>&2
ECHO Usage: %~nx0 [--no-cache] 1>&2
ENDLOCAL & EXIT /B 1

:HELP
ECHO Usage: %~nx0 [--no-cache]
ENDLOCAL & EXIT /B 0

:BUILD
PUSHD "%~dp0"
IF ERRORLEVEL 1 EXIT /B 1

SET "COMPOSE_CMD=docker compose -f docker\docker-compose.yml"
SET "RESULT=0"

%COMPOSE_CMD% build %BUILD_OPTIONS% build
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
