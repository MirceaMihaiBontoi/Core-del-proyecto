# SoterIA Windows Installation Guide

This guide covers installing and running SoterIA on Windows 10/11 (x64).

## System Requirements

### Operating System
- **Windows**: 10 (64-bit, build 1903+) or Windows 11
- **Architecture**: x64 (ARM64 not yet supported)

### Software Requirements
- **Java**: JDK 25 or higher
- **Maven**: 3.9.x or higher
- **PowerShell**: 5.1 or higher (included in Windows 10/11)

### Hardware Requirements
- **RAM**: Minimum 6 GB (8 GB recommended for Balanced profile, 12 GB+ for Expert profile)
- **Disk Space**: 10 GB free space for models and application
- **CPU**: x64 architecture

---

## Quick Start (Recommended)

Open **PowerShell as Administrator** and run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\setup_windows.ps1
```

The script will:
1. Install JDK 25 via `winget` when available (otherwise install manually; see below)
2. Ensure **Apache Maven** via a **portable** install under `%LOCALAPPDATA%\SoterIA\tools` when it is missing from `PATH` (on clean Windows in 2025–2026, `winget` often has no reliable `Apache.Maven` package id)
3. Verify all native libraries are present
4. Register the sherpa-onnx JARs in your local Maven repository
5. Copy DLLs to `%TEMP%`
6. Run **`mvn clean package`** with tests omitted (`maven.test.skip=true`) for a fast runnable build. Full JUnit suite: `mvn verify`. Triage stress driver: **`.\run_test.ps1`** (runs `ClassifierStressTest` only)
7. Launch SoterIA

---

## Manual Installation

If you prefer to install dependencies yourself, follow these steps.

### 1. Install JDK 25

**Option A — winget (recommended):**
```powershell
winget install --id Microsoft.OpenJDK.25
```

**Option B — manual download:**  
Download from [https://jdk.java.net/25/](https://jdk.java.net/25/) and run the installer.

After installation, set `JAVA_HOME` and add it to `PATH`:
```powershell
# In an elevated PowerShell session
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-25", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Program Files\Java\jdk-25\bin", "Machine")
```

Verify:
```powershell
java -version
# Should show version 25 or higher
```

### 2. Install Apache Maven

**Recommended — same as `setup_windows.ps1` (portable):**  
Download the Maven binary zip from [Apache Maven](https://maven.apache.org/download.cgi), extract to e.g. `%LOCALAPPDATA%\SoterIA\tools\apache-maven-3.9.x`, and add the `bin` folder to your user `PATH`.

**Option A — winget (may be unavailable):**  
Some systems no longer list `Apache.Maven`. If `winget search maven` shows a publisher-verified package, you can use it; otherwise prefer the zip above.

**Option B — manual:**  
Extract to `C:\maven` (or any folder) and add `C:\maven\bin` to `PATH`.

Verify:
```powershell
mvn -version
# Should show version 3.9.x or higher
```

### 3. Verify Native Libraries

Ensure the following files are present before running setup:

```
lib\llama\jllama.dll                              ← jllama native library
lib\sherpa-onnx\windows\sherpa-onnx-jni.dll       ← Sherpa-ONNX JNI
lib\sherpa-onnx\windows\onnxruntime.dll           ← ONNX Runtime
lib\sherpa-onnx\sherpa-onnx-v1.13.0.jar           ← Sherpa-ONNX Java API
lib\sherpa-onnx\sherpa-onnx-native-lib-win-x64-v1.13.0.jar
```

If any DLL is missing, see the sections below.

#### jllama.dll

Compile from the jllama fork following `lib\llama\BUILD.md`, or download a pre-built binary.

Quick build (requires Visual Studio Build Tools and CMake):
```powershell
git clone https://github.com/MirceaMihaiBontoi/java-llama.cpp.git
cd java-llama.cpp
mkdir build; cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DLLAMA_NATIVE_ARCH=OFF
cmake --build . --config Release
# Copy the resulting jllama.dll to lib\llama\
```

#### sherpa-onnx DLLs

Download the Windows x64 shared library package from the releases page:

```
https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.0
```

Look for `sherpa-onnx-v1.13.0-win-x64-shared.tar.bz2`, extract it, and copy:
- `sherpa-onnx-jni.dll` → `lib\sherpa-onnx\windows\`
- `onnxruntime.dll`     → `lib\sherpa-onnx\windows\`

### 4. Register JARs in Maven Local Repository

```powershell
# sherpa-onnx Java API
mvn install:install-file `
    -Dfile="lib\sherpa-onnx\sherpa-onnx-v1.13.0.jar" `
    -DgroupId=com.k2fsa.sherpa `
    -DartifactId=sherpa-onnx `
    -Dversion=1.13.0 `
    -Dpackaging=jar

# sherpa-onnx native Windows
mvn install:install-file `
    -Dfile="lib\sherpa-onnx\sherpa-onnx-native-lib-win-x64-v1.13.0.jar" `
    -DgroupId=com.k2fsa.sherpa `
    -DartifactId=sherpa-onnx-native-win-x64 `
    -Dversion=1.13.0 `
    -Dpackaging=jar
```

### 5. Build the Project

```powershell
mvn clean package "-Dmaven.test.skip=true"
```

(On PowerShell, quote the `-D` property so it is not split. To compile tests but skip running them, use `mvn clean package -DskipTests`.)

### 6. Run the Application

```powershell
$sherpaDir = "$PSScriptRoot\lib\sherpa-onnx\windows"
mvn javafx:run `
    "-Djava.library.path=$sherpaDir;$env:TEMP" `
    "-Dde.kherud.llama.lib.path=$env:TEMP"
```

---

## Directory Structure

```
SoterIA\
├── lib\
│   ├── llama\
│   │   ├── jllama.dll                  ← Windows jllama native library
│   │   ├── llama-LOCAL.jar             ← jllama Java API
│   │   └── BUILD.md                    ← Build instructions
│   ├── sherpa-onnx\
│   │   ├── windows\
│   │   │   ├── sherpa-onnx-jni.dll     ← Sherpa-ONNX JNI
│   │   │   └── onnxruntime.dll         ← ONNX Runtime
│   │   ├── sherpa-onnx-v1.13.0.jar
│   │   └── sherpa-onnx-native-lib-win-x64-v1.13.0.jar
│   └── native\
│       ├── sherpa-onnx-jni.dll         ← Fallback DLLs
│       └── onnxruntime.dll
├── src\                                ← Source code
├── target\                             ← Build output
├── pom.xml
├── setup_windows.ps1                   ← This setup script
└── INSTALL_WINDOWS.md                  ← This file
```

---

## Troubleshooting

### `UnsatisfiedLinkError: jllama.dll`

The JVM cannot find `jllama.dll`.

1. Verify the file exists: `Test-Path lib\llama\jllama.dll`
2. Make sure the setup script copied it to `%TEMP%`
3. Pass the directory that contains `jllama.dll` (not the full path to the file; see `de.kherud.llama.LlamaLoader`):
   ```powershell
   mvn javafx:run "-Dde.kherud.llama.lib.path=$env:TEMP"
   ```

### `UnsatisfiedLinkError: sherpa-onnx-jni`

The JVM cannot find `sherpa-onnx-jni.dll`.

1. Verify `lib\sherpa-onnx\windows\sherpa-onnx-jni.dll` exists
2. Ensure `java.library.path` includes that directory:
   ```powershell
   mvn javafx:run "-Djava.library.path=lib\sherpa-onnx\windows;$env:TEMP"
   ```

### `mvn` not recognized

Maven is not in `PATH`. Either:
- Re-open PowerShell after installation so the new `PATH` is loaded
- If you used the setup script, confirm `%LOCALAPPDATA%\SoterIA\tools\apache-maven-*\bin` is on your user `PATH`
- Add Maven's `bin` directory manually:
  ```powershell
  $env:Path += ";C:\maven\bin"
  ```

### `java` not recognized

JDK is not in `PATH`. Either:
- Re-open PowerShell after installation
- Set `JAVA_HOME` and add it to `PATH` manually (see step 1 above)

### PowerShell execution policy error

```
.\setup_windows.ps1 cannot be loaded because running scripts is disabled
```

Run this first:
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

### Audio issues (no microphone / no TTS output)

1. Check that your microphone is enabled in **Settings → Privacy → Microphone**
2. Verify the default audio device in **Sound settings**
3. Run the Windows audio troubleshooter

### JavaFX rendering issues

Try software rendering as a fallback:
```powershell
mvn javafx:run "-Dprism.order=sw"
```

---

## Performance Tuning

### Increase JVM heap for large models

```powershell
$env:MAVEN_OPTS = "-Xmx4g"
mvn javafx:run ...
```

### Limit CPU threads

```powershell
$env:OMP_NUM_THREADS = "4"
mvn javafx:run ...
```

---

## Uninstallation

```powershell
# Remove the application folder
Remove-Item -Recurse -Force "C:\path\to\SoterIA"

# Remove user data (optional)
Remove-Item -Recurse -Force "$env:USERPROFILE\.soteria"
```

---

## Getting Help

- **Build issues**: `lib\llama\BUILD.md`
- **GitHub Issues**: https://github.com/MirceaMihaiBontoi/soteria/issues
- **General info**: `README.md`
