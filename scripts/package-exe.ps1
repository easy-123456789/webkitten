param(
    [string]$Name = "Webkitten",
    [string]$Version = "1.0.0",
    [string]$MainClass = "com.webkitten.browser.Main",
    [string]$OutputDir = "target/exe"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set. Install a JDK 17+ that includes jpackage and then run this script again."
}

$jpackageExe = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
if (-not (Test-Path $jpackageExe)) {
    throw "jpackage.exe was not found at $jpackageExe. Use a JDK with jpackage support (Java 17+)."
}

Write-Host "Building the app jar..."
&mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed."
}

$jarPath = Get-ChildItem -Path "$repoRoot\target" -Filter "webkitten-browser-*.jar" |
    Where-Object { $_.Name -notmatch "original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jarPath) {
    throw "No runnable JAR was found in target/. Check the Maven build output."
}

$dest = Join-Path $repoRoot $OutputDir
New-Item -ItemType Directory -Path $dest -Force | Out-Null

Write-Host "Creating Windows EXE package at $dest"
& $jpackageExe `
    --type exe `
    --name $Name `
    --app-version $Version `
    --vendor "com.webkitten" `
    --input "$repoRoot\target" `
    --main-jar $jarPath.Name `
    --main-class $MainClass `
    --dest $dest `
    --win-menu `
    --win-shortcut `
    --win-menu-group "Webkitten" `
    --java-options "-Dprism.order=sw" `
    --java-options "-Dfile.encoding=UTF-8"

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed to create the EXE."
}

Write-Host "Done. EXE output: $dest\$Name.exe"
