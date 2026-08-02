$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot'
$env:PATH = 'C:\Windows\System32;C:\Windows;C:\Windows\System32\WindowsPowerShell\v1.0'
Set-Location 'C:\Spherical_Insights\Projects\sample-2\sample-refactored\sample'
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "PATH=$env:PATH"
Write-Host "Starting mvnw..."
.\mvnw.cmd -q package -DskipTests
Write-Host "Done mvnw"
