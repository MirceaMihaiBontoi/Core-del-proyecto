#!/bin/sh
# =============================================================================
# SoterIA - Setup and launch script for Linux (POSIX sh)
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()    { printf '%b\n' "${BLUE}[INFO]${NC} $1"; }
success() { printf '%b\n' "${GREEN}[OK]${NC}   $1"; }
warn()    { printf '%b\n' "${YELLOW}[WARN]${NC} $1"; }
error()   { printf '%b\n' "${RED}[ERROR]${NC} $1"; exit 1; }

echo ""
echo "============================================="
echo "   SoterIA - Linux Setup"
echo "============================================="
echo ""

# -----------------------------------------------------------------------------
# 1. Detect distribution
# -----------------------------------------------------------------------------
if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO=$ID
    info "Distribution detected: $PRETTY_NAME"
else
    warn "Could not detect distribution. Assuming Debian/Ubuntu."
    DISTRO="ubuntu"
fi

# -----------------------------------------------------------------------------
# 2. Install system dependencies
# -----------------------------------------------------------------------------
info "Installing system dependencies..."

case "$DISTRO" in
    ubuntu|debian|linuxmint|pop)
        sudo apt-get update -qq
        sudo apt-get install -y \
            openjdk-25-jdk \
            maven \
            libstdc++6 \
            libgomp1 \
            libomp-dev \
            libgcc-s1 \
            wget \
            curl
        ;;
    fedora|rhel|centos|rocky|almalinux)
        sudo dnf install -y \
            java-25-openjdk-devel \
            maven \
            libstdc++ \
            libgomp \
            libomp-devel \
            wget \
            curl
        ;;
    arch|manjaro|endeavouros)
        sudo pacman -Sy --needed --noconfirm \
            jdk25-openjdk \
            maven \
            gcc-libs \
            openmp \
            wget \
            curl
        ;;
    opensuse*|sles)
        sudo zypper install -y \
            java-25-openjdk-devel \
            maven \
            libstdc++6 \
            libgomp1 \
            wget \
            curl
        ;;
    *)
        warn "Distribution '$DISTRO' not recognised. Trying apt-get..."
        sudo apt-get update -qq
        sudo apt-get install -y \
            openjdk-25-jdk \
            maven \
            libstdc++6 \
            libgomp1 \
            wget \
            curl || error "Could not install dependencies. Please install them manually."
        ;;
esac

success "System dependencies installed."

# -----------------------------------------------------------------------------
# 3. Verify Java 25
# -----------------------------------------------------------------------------
info "Checking Java..."

# Point the system default to Java 25 if update-alternatives is available
if command -v update-alternatives >/dev/null 2>&1; then
    JAVA25=$(update-alternatives --list java 2>/dev/null | grep -E "java-25|jdk-25" | head -1 || true)
    if [ -n "$JAVA25" ]; then
        sudo update-alternatives --set java "$JAVA25"
        JAVAC25=$(echo "$JAVA25" | sed 's|/bin/java||')/bin/javac
        [ -f "$JAVAC25" ] && sudo update-alternatives --set javac "$JAVAC25" 2>/dev/null || true
        success "Java 25 set as default: $JAVA25"
    fi
fi

JAVA_VER=$(java -version 2>&1 | head -1)
info "Active Java: $JAVA_VER"

JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -1)
if [ -z "$JAVA_MAJOR" ]; then
    JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\)\.[0-9]*.*/\1/p' | head -1)
fi

if [ "$JAVA_MAJOR" -lt 25 ] 2>/dev/null; then
    error "Java 25 or higher is required. Current version: $JAVA_VER\nDownload JDK 25 from https://jdk.java.net/25/"
fi
success "Java $JAVA_MAJOR detected."

# -----------------------------------------------------------------------------
# 4. Verify Maven
# -----------------------------------------------------------------------------
info "Checking Maven..."
MVN_VER=$(mvn -version 2>&1 | head -1)
success "Maven: $MVN_VER"

# -----------------------------------------------------------------------------
# 5. Verify native libraries
# -----------------------------------------------------------------------------
info "Checking native libraries..."

# jllama
if [ ! -f "${SCRIPT_DIR}/lib/llama/libjllama.so" ]; then
    error "Missing lib/llama/libjllama.so\nBuild it following the instructions in lib/llama/BUILD.md"
fi
success "libjllama.so found."

# sherpa-onnx
SHERPA_DIR="${SCRIPT_DIR}/lib/sherpa-onnx/linux"
for lib in libsherpa-onnx-jni.so libonnxruntime.so; do
    if [ ! -f "${SHERPA_DIR}/${lib}" ]; then
        error "Missing ${SHERPA_DIR}/${lib}\nDownload it from https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.0"
    fi
done
success "sherpa-onnx libraries found."

# Check shared library dependencies
info "Checking native library dependencies (ldd)..."
if ldd "${SCRIPT_DIR}/lib/llama/libjllama.so" 2>&1 | grep -q "not found"; then
    warn "Missing dependencies in libjllama.so:"
    ldd "${SCRIPT_DIR}/lib/llama/libjllama.so" 2>&1 | grep "not found"
fi
if ldd "${SHERPA_DIR}/libsherpa-onnx-jni.so" 2>&1 | grep -q "not found"; then
    warn "Missing dependencies in libsherpa-onnx-jni.so:"
    ldd "${SHERPA_DIR}/libsherpa-onnx-jni.so" 2>&1 | grep "not found"
fi

# -----------------------------------------------------------------------------
# 6. Register sherpa-onnx JARs in the local Maven repository
# -----------------------------------------------------------------------------
info "Registering sherpa-onnx JARs in the local Maven repository..."

SHERPA_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-v1.13.0.jar"
SHERPA_NATIVE_WIN_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-native-lib-win-x64-v1.13.0.jar"
SHERPA_NATIVE_LINUX_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-native-linux-x64-1.13.0.jar"

# sherpa-onnx Java API
if [ -f "$SHERPA_JAR" ]; then
    mvn install:install-file -q \
        -Dfile="$SHERPA_JAR" \
        -DgroupId=com.k2fsa.sherpa \
        -DartifactId=sherpa-onnx \
        -Dversion=1.13.0 \
        -Dpackaging=jar
    success "sherpa-onnx-v1.13.0.jar registered."
else
    error "File not found: $SHERPA_JAR"
fi

# sherpa-onnx-native-win-x64 (needed to resolve the Windows profile in pom.xml, even on Linux)
if [ -f "$SHERPA_NATIVE_WIN_JAR" ]; then
    mvn install:install-file -q \
        -Dfile="$SHERPA_NATIVE_WIN_JAR" \
        -DgroupId=com.k2fsa.sherpa \
        -DartifactId=sherpa-onnx-native-win-x64 \
        -Dversion=1.13.0 \
        -Dpackaging=jar
    success "sherpa-onnx-native-win-x64 registered."
fi

# Download sherpa-onnx-native-linux-x64 if not present
if [ ! -f "$SHERPA_NATIVE_LINUX_JAR" ]; then
    info "Downloading sherpa-onnx-native-linux-x64-1.13.0.jar..."
    SHERPA_LINUX_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.0/sherpa-onnx-v1.13.0-linux-x64-jni.jar"
    wget -q --show-progress -O "$SHERPA_NATIVE_LINUX_JAR" "$SHERPA_LINUX_URL" || {
        warn "Download failed. Building JAR from local .so files..."
        TMPDIR_JAR=$(mktemp -d)
        mkdir -p "${TMPDIR_JAR}/linux-x86-64"
        cp "${SHERPA_DIR}"/*.so "${TMPDIR_JAR}/linux-x86-64/"
        jar cf "$SHERPA_NATIVE_LINUX_JAR" -C "$TMPDIR_JAR" .
        rm -rf "$TMPDIR_JAR"
        success "Native Linux JAR built from local libraries."
    }
fi

mvn install:install-file -q \
    -Dfile="$SHERPA_NATIVE_LINUX_JAR" \
    -DgroupId=com.k2fsa.sherpa \
    -DartifactId=sherpa-onnx-native-linux-x64 \
    -Dversion=1.13.0 \
    -Dpackaging=jar
success "sherpa-onnx-native-linux-x64 registered."

# -----------------------------------------------------------------------------
# 7. Copy native libraries to /tmp
# -----------------------------------------------------------------------------
info "Copying native libraries to /tmp..."
cp "${SHERPA_DIR}"/*.so /tmp/
cp "${SCRIPT_DIR}/lib/llama/libjllama.so" /tmp/libjllama.so

# The sherpa-onnx JAR calls System.loadLibrary("sherpa-onnx-jni"), which looks for
# "libsherpa-onnx-jni.so" on java.library.path. Also create a symlink with underscores
# in case the JAR uses "sherpa_onnx_jni" (some versions do).
ln -sf /tmp/libsherpa-onnx-jni.so /tmp/libsherpa_onnx_jni.so 2>/dev/null || true

success "Libraries copied to /tmp."

# -----------------------------------------------------------------------------
# 8. Configure audio (PulseAudio / PipeWire / WSLg)
# -----------------------------------------------------------------------------
if [ -S "/mnt/wslg/PulseServer" ]; then
    export PULSE_SERVER="unix:/mnt/wslg/PulseServer"
    info "Audio: WSLg PulseAudio detected → $PULSE_SERVER"
else
    info "Audio: using system audio server."
fi

# -----------------------------------------------------------------------------
# 9. Build the project
# -----------------------------------------------------------------------------
info "Building SoterIA with Maven..."
export LD_LIBRARY_PATH="/tmp:${LD_LIBRARY_PATH}"

mvn clean package -DskipTests -q \
    || error "Build failed. Check the Maven output above."

success "Build successful."

# -----------------------------------------------------------------------------
# 10. Ensure writable log directories
# -----------------------------------------------------------------------------
info "Preparing log directories..."

LOG_ROOT="${SCRIPT_DIR}/logs"

mkdir -p "${LOG_ROOT}/voice" "${LOG_ROOT}/raw_llm" "${LOG_ROOT}/raw_classifier"

if [ ! -w "$LOG_ROOT" ] || [ ! -w "${LOG_ROOT}/voice" ]; then
    warn "logs/ is not writable by $(whoami). Attempting to fix ownership..."
    if sudo chown -R "$(id -u)":"$(id -g)" "$LOG_ROOT"; then
        success "logs/ ownership fixed."
    else
        warn "Could not fix logs/ ownership. Diagnostic logs may fail at runtime."
    fi
fi

chmod -R u+rwX "$LOG_ROOT" 2>/dev/null || chmod -R 755 "$LOG_ROOT"

success "Log directories ready."

# -----------------------------------------------------------------------------
# 11. Launch the application
# -----------------------------------------------------------------------------
echo ""
echo "============================================="
success "All done. Starting SoterIA..."
echo "============================================="
echo ""

export LD_LIBRARY_PATH="/tmp:${LD_LIBRARY_PATH}"

mvn javafx:run \
    -Djava.library.path="/tmp:${SCRIPT_DIR}/lib/sherpa-onnx/linux" \
    -Dde.kherud.llama.lib.path=/tmp
