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

## Required System Packages

SoterIA requires several system libraries to run native components (jllama, Sherpa-ONNX).

### Ubuntu / Debian

```bash
sudo apt-get update
sudo apt-get install -y \
    libstdc++6 \
    libgomp1 \
    libomp-dev \
    openjdk-25-jdk \
    maven
```

### Fedora / RHEL / CentOS

```bash
sudo dnf install -y \
    libstdc++ \
    libgomp \
    libomp-devel \
    java-25-openjdk-devel \
    maven
```

### Arch Linux

```bash
sudo pacman -S --needed \
    gcc-libs \
    openmp \
    jdk25-openjdk \
    maven
```

## Installation Steps

### Option 1: Binary Distribution (Recommended)

1. **Download the latest release**:
   ```bash
   # Download from GitHub releases (when available)
   wget https://github.com/MirceaMihaiBontoi/soteria/releases/latest/soteria-linux-x64.tar.gz
   ```

2. **Extract the archive**:
   ```bash
   tar -xzf soteria-linux-x64.tar.gz
   cd soteria
   ```

3. **Verify native libraries**:
   ```bash
   ls -la lib/llama/jllama.so
   ls -la lib/sherpa-onnx/linux/
   ```

4. **Run the application**:
   ```bash
   ./run.sh
   ```

### Option 2: Build from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/MirceaMihaiBontoi/soteria.git
   cd soteria
   ```

2. **Compile jllama for Linux** (if not already present):
   
   See `lib/llama/BUILD.md` for detailed instructions. Quick version:
   
   ```bash
   # Clone the jllama fork
   git clone https://github.com/MirceaMihaiBontoi/java-llama.cpp.git
   cd java-llama.cpp
   
   # Build
   mkdir build && cd build
   cmake .. -DCMAKE_BUILD_TYPE=Release \
            -DLLAMA_NATIVE_ARCH=OFF \
            -DLLAMA_CUDA=OFF \
            -DLLAMA_METAL=OFF
   make -j$(nproc)
   
   # Copy to SoterIA
   cp libjllama.so /path/to/soteria/lib/llama/jllama.so
   cd /path/to/soteria
   ```

3. **Download Sherpa-ONNX binaries** (if not already present):
   
   See `lib/sherpa-onnx/linux/README.md` for detailed instructions. Quick version:
   
   ```bash
   # Download
   wget https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.0/sherpa-onnx-v1.13.0-linux-x64-shared.tar.bz2
   
   # Extract
   tar -xjf sherpa-onnx-v1.13.0-linux-x64-shared.tar.bz2
   
   # Copy libraries
   cp sherpa-onnx-v1.13.0-linux-x64-shared/lib/libsherpa-onnx-jni.so lib/sherpa-onnx/linux/
   cp sherpa-onnx-v1.13.0-linux-x64-shared/lib/libonnxruntime.so lib/sherpa-onnx/linux/
   ```

4. **Build the application**:
   ```bash
   chmod +x build.sh
   ./build.sh
   ```

5. **Run the application**:
   ```bash
   mvn javafx:run
   ```

## Verifying Installation

### Check Native Libraries

```bash
# Verify jllama
ls -lh lib/llama/jllama.so
ldd lib/llama/jllama.so

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

## File Permissions

For security, native libraries should be read-only:

```bash
# Set library permissions
chmod 444 lib/llama/*.so
chmod 444 lib/sherpa-onnx/linux/*.so

# Set directory permissions
chmod 555 lib/
chmod 555 lib/llama/
chmod 555 lib/sherpa-onnx/
chmod 555 lib/sherpa-onnx/linux/
```

## Troubleshooting

### Issue: `UnsatisfiedLinkError: jllama.so`

**Cause**: jllama native library is missing or not in the correct location.

**Solution**:
1. Verify the file exists: `ls -la lib/llama/jllama.so`
2. If missing, compile from the fork (see `lib/llama/BUILD.md`)
3. Ensure file permissions allow reading: `chmod 444 lib/llama/jllama.so`

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

**Cause**: File permissions are too restrictive.

**Solution**:
```bash
chmod +x build.sh
chmod +x lib/llama/*.so
chmod +x lib/sherpa-onnx/linux/*.so
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

## Directory Structure

```
soteria/
├── lib/
│   ├── llama/
│   │   ├── jllama.so              # Linux jllama native library
│   │   └── BUILD.md               # Build instructions
│   ├── sherpa-onnx/
│   │   ├── linux/
│   │   │   ├── libsherpa-onnx-jni.so
│   │   │   ├── libonnxruntime.so
│   │   │   └── README.md          # Download instructions
│   │   └── windows/               # Windows libraries (ignored on Linux)
│   └── native/                    # Fallback directory
├── src/                           # Source code
├── target/                        # Build output
├── pom.xml                        # Maven configuration
├── build.sh                       # Linux build script
└── INSTALL_LINUX.md              # This file
```

## Performance Tuning

### Memory Configuration

For better performance with large models:

```bash
export MAVEN_OPTS="-Xmx4g"
mvn javafx:run
```

### CPU Optimization

jllama and Sherpa-ONNX will automatically use available CPU cores. For manual control:

```bash
export OMP_NUM_THREADS=4  # Limit to 4 threads
mvn javafx:run
```

## Uninstallation

To remove SoterIA:

```bash
# Remove application directory
rm -rf /path/to/soteria

# Remove user data (optional)
rm -rf ~/.soteria
```

## Getting Help

- **Documentation**: See `README.md` for general information
- **Build Issues**: Check `lib/llama/BUILD.md` and `lib/sherpa-onnx/linux/README.md`
- **GitHub Issues**: https://github.com/MirceaMihaiBontoi/soteria/issues

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

1. Run the application: `mvn javafx:run`
2. Complete the onboarding wizard
3. Select your AI model profile (Lite/Balanced/Expert)
4. Configure your emergency profile
5. Test voice input and output

For more information, see the main `README.md` file.
