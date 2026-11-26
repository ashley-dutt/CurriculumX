@echo off
setlocal
set "ROOT=%~dp0..\"
cd /d "%ROOT%"

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
)
set "PATH=%JAVA_HOME%\bin;%ROOT%.tools\apache-maven-3.9.9\bin;%PATH%"

echo ======================================================================
echo  Starting CurriculumX on Java and Spring Boot...
echo ======================================================================

if exist "%ROOT%backend-spring\target\curriculumx-backend-1.0.0.jar" (
    "%JAVA_HOME%\bin\java.exe" -jar "%ROOT%backend-spring\target\curriculumx-backend-1.0.0.jar"
) else (
    cd "%ROOT%backend-spring"
    call "%ROOT%.tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
)
