FROM mcr.microsoft.com/windows/servercore:latest

COPY install_tools.ps1 c:/
RUN powershell -Command "C:\install_tools.ps1"
RUN setx /M MAVEN_OPTS "-Xmx1048m -Xms256m -XX:MaxPermSize=312M"
