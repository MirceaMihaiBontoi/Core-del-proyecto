# =============================================================================
# SoterIA - Setup and launch script for Windows
# Run in PowerShell as Administrator:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
#   .\setup_windows.ps1
# =============================================================================
#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

# -----------------------------------------------------------------------------
# Color helpers
# -----------------------------------------------------------------------------
function Info    { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Success { param($msg) Write-Host "[OK]    $msg" -ForegroundColor Green }
function Warn    { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Err     { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

# -----------------------------------------------------------------------------
# Native CLI helpers (PowerShell maps java -version stderr to ErrorRecord — use cmd)
# -----------------------------------------------------------------------------
function Get-JavaVersionLine {
    $line = (cmd /c "java -version 2>&1") | Select-Object -First 1
    if ($line -is [string]) { return $line.Trim() }
    return [string]$line
}

function Get-MvnVersionLine {
    $line = (cmd /c "mvn -version 2>&1") | Select-Object -First 1
    if ($line -is [string]) { return $line.Trim() }
    return [string]$line
}

# Apache Maven is often missing from winget community manifests on a fresh Windows install; use the official binary zip.
function Ensure-MavenPortable {
    param(
        [string]$Version = "3.9.9"
    )
    if ($null -ne (Get-Command mvn -ErrorAction SilentlyContinue)) {
        return
    }

    $toolsRoot = Join-Path $env:LOCALAPPDATA "SoterIA\tools"
    $mavenHome = Join-Path $toolsRoot "apache-maven-$Version"
    $mavenBin  = Join-Path $mavenHome "bin"

    if (-not (Test-Path (Join-Path $mavenBin "mvn.cmd"))) {
        New-Item -ItemType Directory -Path $toolsRoot -Force | Out-Null
        $zip = Join-Path $env:TEMP "apache-maven-$Version-bin.zip"
        $urls = @(
            "https://dlcdn.apache.org/maven/maven-3/$Version/binaries/apache-maven-$Version-bin.zip",
            "https://archive.apache.org/dist/maven/maven-3/$Version/binaries/apache-maven-$Version-bin.zip"
        )
        Info "Downloading Apache Maven $Version (portable)..."
        $ok = $false
        foreach ($url in $urls) {
            try {
                Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
                $ok = $true
                break
            }
            catch {
                continue
            }
        }
        if (-not $ok) {
            Err "Could not download Maven. Install manually from https://maven.apache.org/download.cgi"
        }
        Expand-Archive -Path $zip -DestinationPath $toolsRoot -Force
        Remove-Item $zip -Force -ErrorAction SilentlyContinue
        if (-not (Test-Path (Join-Path $mavenBin "mvn.cmd"))) {
            Err "Maven extraction failed under $toolsRoot"
        }
        Success "Maven $Version installed to $mavenHome"
    }

    if ($env:Path -notlike "*$mavenBin*") {
        $env:Path = "$mavenBin;$env:Path"
    }
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ([string]::IsNullOrEmpty($userPath)) {
        [Environment]::SetEnvironmentVariable("Path", $mavenBin, "User")
    }
    elseif ($userPath -notlike "*$mavenBin*") {
        [Environment]::SetEnvironmentVariable("Path", "$userPath;$mavenBin", "User")
    }
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   SoterIA - Windows Setup"                  -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# -----------------------------------------------------------------------------
# 1. Check for Administrator privileges
# -----------------------------------------------------------------------------
$isAdmin = ([Security.Principal.WindowsPrincipal] `
    [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Warn "This script is not running as Administrator."
    Warn "Some installations (winget, global environment variables) may fail."
    Warn "Restart PowerShell as Administrator if you encounter errors."
    Write-Host ""
}

# -----------------------------------------------------------------------------
# 2. Install dependencies via winget (if available)
# -----------------------------------------------------------------------------
Info "Checking for winget package manager..."

$wingetAvailable = $null -ne (Get-Command winget -ErrorAction SilentlyContinue)

if ($wingetAvailable) {
    Success "winget found."

    # Java 25 (Microsoft Build of OpenJDK or Adoptium)
    Info "Installing JDK 25..."
    winget install --id Microsoft.OpenJDK.25 --accept-source-agreements --accept-package-agreements --silent 2>$null
    if ($LASTEXITCODE -ne 0) {
        Warn "Could not install JDK 25 via winget. Trying Adoptium..."
        winget install --id EclipseAdoptium.Temurin.25.JDK --accept-source-agreements --accept-package-agreements --silent 2>$null
        if ($LASTEXITCODE -ne 0) {
            Warn "Automatic JDK installation failed. Download manually from:"
            Warn "  https://jdk.java.net/25/"
        }
    }

} else {
    Warn "winget is not available. Please install JDK 25 manually if needed:"
    Warn "  - JDK 25: https://jdk.java.net/25/"
    Warn "Maven will be installed as a portable build under %LOCALAPPDATA%\\SoterIA\\tools if missing."
}

# -----------------------------------------------------------------------------
# 3. Refresh PATH for the current session
# -----------------------------------------------------------------------------
Info "Refreshing PATH for the current session..."
$machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
$userPath    = [System.Environment]::GetEnvironmentVariable("Path", "User")
$env:Path    = "$machinePath;$userPath"

# Maven: winget often has no stable package id on clean Windows (2025–2026); use Apache distribution.
Info "Ensuring Apache Maven..."
Ensure-MavenPortable -Version "3.9.9"

$machinePath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
$userPath    = [System.Environment]::GetEnvironmentVariable("Path", "User")
$env:Path    = "$machinePath;$userPath"

# -----------------------------------------------------------------------------
# 4. Verify Java 25
# -----------------------------------------------------------------------------
Info "Checking Java..."

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $javaCmd) {
    Err "Java not found in PATH.`nInstall JDK 25 from https://jdk.java.net/25/ and add JAVA_HOME to PATH."
}

$javaVersionOutput = Get-JavaVersionLine
Info "Active Java: $javaVersionOutput"

$javaMajor = [regex]::Match($javaVersionOutput, '"(\d+)').Groups[1].Value
if ([int]$javaMajor -lt 25) {
    Err "Java 25 or higher is required. Current version: $javaVersionOutput`nDownload JDK 25 from https://jdk.java.net/25/"
}
Success "Java $javaMajor detected."

# -----------------------------------------------------------------------------
# 5. Verify Maven
# -----------------------------------------------------------------------------
Info "Checking Maven..."
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -eq $mvnCmd) {
    Err "Maven not found in PATH.`nInstall Maven from https://maven.apache.org/download.cgi"
}
$mvnVersion = Get-MvnVersionLine
Success "Maven: $mvnVersion"

# -----------------------------------------------------------------------------
# 6. Verify native libraries
# -----------------------------------------------------------------------------
Info "Checking native libraries..."

# jllama
$jllamaDll = Join-Path $ScriptDir "lib\llama\jllama.dll"
if (-not (Test-Path $jllamaDll)) {
    Err "Missing lib\llama\jllama.dll`nBuild it following the instructions in lib\llama\BUILD.md"
}
Success "jllama.dll found."

# sherpa-onnx
$sherpaDir = Join-Path $ScriptDir "lib\sherpa-onnx\windows"
foreach ($dll in @("sherpa-onnx-jni.dll", "onnxruntime.dll")) {
    $dllPath = Join-Path $sherpaDir $dll
    if (-not (Test-Path $dllPath)) {
        Err "Missing $sherpaDir\$dll`nDownload it from https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.0"
    }
}
Success "sherpa-onnx libraries found."

# -----------------------------------------------------------------------------
# 7. Register sherpa-onnx JARs in the local Maven repository
# -----------------------------------------------------------------------------
Info "Registering sherpa-onnx JARs in the local Maven repository..."

$sherpaJar         = Join-Path $ScriptDir "lib\sherpa-onnx\sherpa-onnx-v1.13.0.jar"
$sherpaNativeWin   = Join-Path $ScriptDir "lib\sherpa-onnx\sherpa-onnx-native-lib-win-x64-v1.13.0.jar"
$sherpaNativeLinux = Join-Path $ScriptDir "lib\sherpa-onnx\sherpa-onnx-native-linux-x64-1.13.0.jar"

# sherpa-onnx Java API
if (-not (Test-Path $sherpaJar)) {
    Err "File not found: $sherpaJar"
}
& mvn install:install-file -q `
    "-Dfile=$sherpaJar" `
    "-DgroupId=com.k2fsa.sherpa" `
    "-DartifactId=sherpa-onnx" `
    "-Dversion=1.13.0" `
    "-Dpackaging=jar"
Success "sherpa-onnx-v1.13.0.jar registered."

# sherpa-onnx-native-win-x64
if (Test-Path $sherpaNativeWin) {
    & mvn install:install-file -q `
        "-Dfile=$sherpaNativeWin" `
        "-DgroupId=com.k2fsa.sherpa" `
        "-DartifactId=sherpa-onnx-native-win-x64" `
        "-Dversion=1.13.0" `
        "-Dpackaging=jar"
    Success "sherpa-onnx-native-win-x64 registered."
} else {
    Warn "File not found: $sherpaNativeWin — skipping (not required on Windows if DLLs are present)."
}

# sherpa-onnx-native-linux-x64 (needed to resolve the Linux profile in pom.xml)
if (Test-Path $sherpaNativeLinux) {
    & mvn install:install-file -q `
        "-Dfile=$sherpaNativeLinux" `
        "-DgroupId=com.k2fsa.sherpa" `
        "-DartifactId=sherpa-onnx-native-linux-x64" `
        "-Dversion=1.13.0" `
        "-Dpackaging=jar"
    Success "sherpa-onnx-native-linux-x64 registered."
} else {
    Warn "File not found: $sherpaNativeLinux — creating empty stub JAR..."
    $tmpDir = Join-Path $env:TEMP "sherpa-linux-stub"
    New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null
    $stubJar = Join-Path $ScriptDir "lib\sherpa-onnx\sherpa-onnx-native-linux-x64-1.13.0.jar"
    & jar cf $stubJar -C $tmpDir .
    & mvn install:install-file -q `
        "-Dfile=$stubJar" `
        "-DgroupId=com.k2fsa.sherpa" `
        "-DartifactId=sherpa-onnx-native-linux-x64" `
        "-Dversion=1.13.0" `
        "-Dpackaging=jar"
    Remove-Item $tmpDir -Recurse -Force
    Success "Linux stub JAR registered."
}

# -----------------------------------------------------------------------------
# 8. Copy DLLs to the Windows temp directory
# -----------------------------------------------------------------------------
Info "Copying native DLLs to $env:TEMP..."

Copy-Item (Join-Path $sherpaDir "sherpa-onnx-jni.dll") $env:TEMP -Force
Copy-Item (Join-Path $sherpaDir "onnxruntime.dll")     $env:TEMP -Force
Copy-Item $jllamaDll                                   $env:TEMP -Force

# Also copy any DLLs from lib\native if present (fallback)
$nativeDir = Join-Path $ScriptDir "lib\native"
if (Test-Path $nativeDir) {
    Get-ChildItem "$nativeDir\*.dll" | ForEach-Object {
        Copy-Item $_.FullName $env:TEMP -Force
    }
}

Success "DLLs copied to $env:TEMP."

# -----------------------------------------------------------------------------
# 9. Build (tests omitted for speed; use mvn verify or .\run_test.ps1 separately)
# -----------------------------------------------------------------------------
# Produces a runnable package. JUnit is not required for compile or javafx:run.
# run_test.ps1 runs only the triage ClassifierStressTest.
Info "Building SoterIA with Maven (clean package, tests omitted)..."

$env:Path = "$sherpaDir;$env:TEMP;$env:Path"
if ([string]::IsNullOrEmpty($env:MAVEN_OPTS)) {
    $env:MAVEN_OPTS = "--enable-native-access=ALL-UNNAMED"
} elseif ($env:MAVEN_OPTS -notlike "*enable-native-access*") {
    $env:MAVEN_OPTS = "$env:MAVEN_OPTS --enable-native-access=ALL-UNNAMED"
}

# Property must be quoted in PowerShell or "-D..." is parsed incorrectly.
& mvn clean package "-Dmaven.test.skip=true" -q
if ($LASTEXITCODE -ne 0) {
    Err "Build failed. Check the Maven output above."
}
Success "Build successful."

# -----------------------------------------------------------------------------
# 10. Launch the application
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "   All done. Starting SoterIA..."            -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""

$libraryPath = "$sherpaDir;$env:TEMP"

& mvn javafx:run `
    "-Djava.library.path=$libraryPath" `
    "-Dde.kherud.llama.lib.path=$env:TEMP\jllama.dll"
