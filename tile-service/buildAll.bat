@set currentDir=%CD%
@set scriptDir=%~dp0
@cd %scriptDir%
@call :Build ..\..\imageop %*
@call :Build ..\..\photogrammetry %*
@call :Build ..\..\leidos-geoint-services -N %*
@call :Build ..\..\leidos-geoint-services %*
@cd %currentDir%
@exit /b 0

:Build
@pushd %1
call %MAVEN_HOME%\bin\mvn %~2 %~3 %~4 %~5 %~6 %~7 %~8 %~9
@popd 
@exit /b 0
