$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
Set-Location $rootDir

$javaHome = "C:\Program Files\Java\jdk-17"
if (-not (Test-Path "$javaHome\bin\java.exe")) {
    $javaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
}
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$rootDir\.tools\apache-maven-3.9.9\bin;$env:PATH"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Starting CurriculumX on Java & Spring Boot..." -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan

$jarPath = "$rootDir\backend-spring\target\curriculumx-backend-1.0.0.jar"
if (Test-Path $jarPath) {
    & "$javaHome\bin\java.exe" -jar $jarPath
} else {
    Set-Location "$rootDir\backend-spring"
    & "$rootDir\.tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
}
