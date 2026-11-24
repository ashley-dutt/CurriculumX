@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;%~dp0\.tools\apache-maven-3.9.9\bin;%PATH%"

echo ======================================================================
echo  Starting CurriculumX on Java & Spring Boot...
echo  Java Version:
java -version
echo ======================================================================

cd backend-spring
call "%~dp0\.tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
