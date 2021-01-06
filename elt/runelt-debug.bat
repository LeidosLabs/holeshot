@echo off
setlocal ENABLEDELAYEDEXPANSION
IF DEFINED JAVA_HOME (
"%JAVA_HOME%\bin\java.exe" ^
   -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8000 ^
   -Xmx4g ^
   -Djava.util.concurrent.ForkJoinPool.common.parallelism=10 ^
   -Dsun.awt.noerasebackground=true ^
   -Dsun.java2d.noddraw=true ^
   -Dfile.encoding=UTF-8 ^
   -jar target/elt.jar
) ELSE (
ECHO JAVA_HOME is not defined
pause
)
