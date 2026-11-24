$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Test-Path "$scriptDir\backend-spring")) {
    if (Test-Path "$scriptDir\..\backend-spring") {
        $scriptDir = Split-Path -Parent $scriptDir
    }
}
Set-Location $scriptDir

$javaHome = "C:\Program Files\Java\jdk-17"
if (-not (Test-Path "$javaHome\bin\java.exe")) {
    $javaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
}
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$scriptDir\.tools\apache-maven-3.9.9\bin;$env:PATH"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Starting CurriculumX on Java & Spring Boot..." -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan

$jarPath = "$scriptDir\backend-spring\target\curriculumx-backend-1.0.0.jar"
if (Test-Path $jarPath) {
    & "$javaHome\bin\java.exe" -jar $jarPath
} else {
    Set-Location "$scriptDir\backend-spring"
    & "$scriptDir\.tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
}
