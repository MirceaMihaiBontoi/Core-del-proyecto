# SoterIA Linux Installation Guide

This guide provides detailed instructions for installing and running SoterIA on Linux systems.

## System Requirements

### Operating System
- **Linux Kernel**: 4.15 or higher
- **Distributions**: Ubuntu 18.04+, Fedora 28+, Debian 10+, Arch Linux, or equivalent
- **glibc**: 2.27 or higher

### Software Requirements
- **Java**: JDK 25 or higher (OpenJDK or Oracle JDK)
- **Maven**: 3.9.x or higher (for building from source)
- **Display Server**: X11 or Wayland

### Hardware Requirements
- **RAM**: Minimum 6 GB (8 GB recommended for Balanced profile, 12 GB+ for Expert profile)
- **Disk Space**: 10 GB free space for models and application
- **CPU**: x64 architecture (ARM64 support planned for future)

---

## Quick Start (Recommended)

The easiest way to install and launch SoterIA is with the provided setup script. It installs all system dependencies, registers the Maven JARs, compiles the project, and starts the application in one step.

```bash
chmod +x setup_linux.sh
./setup_linux.sh
```

The script requires `sudo` to install system packages and auto-detects your distribution (Ubuntu/Debian, Fedora/RHEL, Arch, openSUSE). If anything is missing it will tell you exactly what to fix before continuing.

---

## Manual Installation

If you prefer to install dependencies yourself, follow the steps below.

### 1. Install System Packages

SoterIA requires several system libraries to run native components (jllama, Sherpa-ONNX).

#### Ubuntu / Debian

```bash
sudo apt-get update
sudo apt-get install -y \
    libstdc++6 \
    libgomp1 \
    libomp-dev \
    openjdk-25-jdk \
    maven
```

#### Fedora / RHEL / CentOS

```bash
sudo dnf install -y \
    libstdc++ \
    libgomp \
    libomp-devel \
    java-25-openjdk-devel \
    maven
```

#### Arch Linux

```bash
sudo pacman -S --needed \
    gcc-libs \
    openmp \
    jdk25-openjdk \
    maven
```

### 2. Verify Native Libraries

Ensure the following files are present before building:

```
lib/llama/libjllama.so                        ← jllama native library
lib/sherpa-onnx/linux/libsherpa-onnx-jni.so   ← Sherpa-ONNX JNI
lib/sherpa-onnx/linux/libonnxruntime.so        ← ONNX Runtime
lib/sherpa-onnx/sherpa-onnx-v1.13.0.jar        ← Sherpa-ONNX Java API
```

If any `.so` is missing, see the sections below.

#### libjllama.so

Compile from the jllama fork following `lib/llama/BUILD.md`. Quick version:

```bash
git clone https://github.com/MirceaMihaiBontoi/java-llama.cpp.git
cd java-llama.cpp
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release \
         -DLLAMA_NATIVE_ARCH=OFF \
         -DLLAMA_CUDA=OFF \
         -DLLAMA_METAL=OFF
make -j$(nproc)
cp libjllama.so /path/to/soteria/lib/llama/
```

#### Sherpa-ONNX libraries

Download the Linux x64 shared library package from the releases page:

```
https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.0
```

Look for `sherpa-onnx-v1.13.0-linux-x64-shared.tar.bz2`, extract it, and copy:

```bash
tar -xjf sherpa-onnx-v1.13.0-linux-x64-shared.tar.bz2
cp sherpa-onnx-v1.13.0-linux-x64-shared/lib/libsherpa-onnx-jni.so lib/sherpa-onnx/linux/
cp sherpa-onnx-v1.13.0-linux-x64-shared/lib/libonnxruntime.so     lib/sherpa-onnx/linux/
```

### 3. Register JARs in Maven Local Repository

```bash
# sherpa-onnx Java API
mvn install:install-file \
    -Dfile=lib/sherpa-onnx/sherpa-onnx-v1.13.0.jar \
    -DgroupId=com.k2fsa.sherpa \
    -DartifactId=sherpa-onnx \
    -Dversion=1.13.0 \
    -Dpackaging=jar

# sherpa-onnx native Linux
mvn install:install-file \
    -Dfile=lib/sherpa-onnx/sherpa-onnx-native-linux-x64-1.13.0.jar \
    -DgroupId=com.k2fsa.sherpa \
    -DartifactId=sherpa-onnx-native-linux-x64 \
    -Dversion=1.13.0 \
    -Dpackaging=jar
```

### 4. Build the Project

```bash
export LD_LIBRARY_PATH="/tmp:$LD_LIBRARY_PATH"
mvn clean package -DskipTests
```

### 5. Run the Application

```bash
mvn javafx:run \
    -Djava.library.path="/tmp:lib/sherpa-onnx/linux" \
    -Dde.kherud.llama.lib.path=/tmp/libjllama.so
```

---

## Verifying Installation

### Check Native Libraries

```bash
# Verify jllama
ls -lh lib/llama/libjllama.so
ldd lib/llama/libjllama.so

# Verify Sherpa-ONNX
ls -lh lib/sherpa-onnx/linux/
ldd lib/sherpa-onnx/linux/libsherpa-onnx-jni.so
ldd lib/sherpa-onnx/linux/libonnxruntime.so
```

### Check Java Version

```bash
java -version
# Should show version 25 or higher
```

### Check Maven Version

```bash
mvn -version
# Should show version 3.9.x or higher
```

---

## File Permissions

For security, native libraries should be read-only:

```bash
chmod 444 lib/llama/*.so
chmod 444 lib/sherpa-onnx/linux/*.so
chmod 555 lib/ lib/llama/ lib/sherpa-onnx/ lib/sherpa-onnx/linux/
```

---

## Directory Structure

```
soteria/
├── lib/
│   ├── llama/
│   │   ├── libjllama.so           # Linux jllama native library
│   │   └── BUILD.md               # Build instructions
│   ├── sherpa-onnx/
│   │   ├── linux/
│   │   │   ├── libsherpa-onnx-jni.so
│   │   │   └── libonnxruntime.so
│   │   └── windows/               # Windows libraries (ignored on Linux)
│   └── native/                    # Fallback directory
├── src/                           # Source code
├── target/                        # Build output
├── pom.xml                        # Maven configuration
├── setup_linux.sh                 # Automated setup & launch script
└── INSTALL_LINUX.md               # This file
```

---

## Troubleshooting

### Issue: `UnsatisfiedLinkError: libjllama.so`

**Cause**: jllama native library is missing or not in the correct location.

**Solution**:
1. Verify the file exists: `ls -la lib/llama/libjllama.so`
2. If missing, compile from the fork (see `lib/llama/BUILD.md`)
3. Ensure file permissions allow reading: `chmod 444 lib/llama/libjllama.so`

### Issue: `libonnxruntime.so: cannot open shared object file`

**Cause**: Missing system dependency (libgomp1).

**Solution**:
```bash
# Ubuntu/Debian
sudo apt-get install libgomp1

# Fedora/RHEL
sudo dnf install libgomp

# Arch
sudo pacman -S openmp
```

### Issue: `GLIBC_2.27 not found`

**Cause**: System glibc version is too old.

**Solution**: Upgrade to a newer Linux distribution:
- Ubuntu 18.04 or higher
- Fedora 28 or higher
- Debian 10 or higher

### Issue: `Permission denied` when running

**Cause**: Script is not executable.

**Solution**:
```bash
chmod +x setup_linux.sh
```

### Issue: Application starts but no audio input/output

**Cause**: Missing audio system or permissions.

**Solution**:
1. Ensure PulseAudio or PipeWire is running
2. Check microphone permissions
3. Test audio: `arecord -l` and `aplay -l`

### Issue: JavaFX not rendering correctly

**Cause**: Missing display server or graphics drivers.

**Solution**:
1. Ensure X11 or Wayland is running
2. Update graphics drivers
3. Try software rendering: `export PRISM_ORDER=sw`

---

## Performance Tuning

### Increase JVM heap for large models

```bash
export MAVEN_OPTS="-Xmx4g"
mvn javafx:run
```

### Limit CPU threads

```bash
export OMP_NUM_THREADS=4
mvn javafx:run
```

---

## Uninstallation

```bash
# Remove application directory
rm -rf /path/to/soteria

# Remove user data (optional)
rm -rf ~/.soteria
```

---

## Getting Help

- **Build issues**: `lib/llama/BUILD.md`
- **GitHub Issues**: https://github.com/MirceaMihaiBontoi/soteria/issues
- **General info**: `README.md`

## Notes

- SoterIA is designed for single-user desktop use
- All AI processing happens locally (no internet required after installation)
- Models are downloaded to `~/.soteria/models/` on first run
- Emergency protocols are stored in `src/main/resources/data/protocols/`

## Security Considerations

- Native libraries should be verified before use
- Keep system packages updated
- Run SoterIA as a regular user (not root)
- Review file permissions regularly

## Next Steps

After installation:

1. Run the application: `./setup_linux.sh` or `mvn javafx:run`
2. Complete the onboarding wizard
3. Select your AI model profile (Lite/Balanced/Expert)
4. Configure your emergency profile
5. Test voice input and output

For more information, see the main `README.md` file.
