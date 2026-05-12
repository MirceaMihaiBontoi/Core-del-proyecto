#!/bin/bash
# =============================================================================
# SoterIA - Script de instalación y arranque para Linux
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC}   $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo ""
echo "============================================="
echo "   SoterIA - Instalación Linux"
echo "============================================="
echo ""

# -----------------------------------------------------------------------------
# 1. Detectar distribución
# -----------------------------------------------------------------------------
if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO=$ID
    info "Distribución detectada: $PRETTY_NAME"
else
    warn "No se pudo detectar la distribución. Asumiendo Debian/Ubuntu."
    DISTRO="ubuntu"
fi

# -----------------------------------------------------------------------------
# 2. Instalar dependencias del sistema
# -----------------------------------------------------------------------------
info "Instalando dependencias del sistema..."

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
        warn "Distribución '$DISTRO' no reconocida. Intentando con apt-get..."
        sudo apt-get update -qq
        sudo apt-get install -y \
            openjdk-25-jdk \
            maven \
            libstdc++6 \
            libgomp1 \
            wget \
            curl || error "No se pudieron instalar las dependencias. Instálalas manualmente."
        ;;
esac

success "Dependencias del sistema instaladas."

# -----------------------------------------------------------------------------
# 3. Verificar Java 25
# -----------------------------------------------------------------------------
info "Verificando Java..."

# Asegurarse de que java apunta a la versión 25
if command -v update-alternatives &>/dev/null; then
    # Buscar java 25 instalado
    JAVA25=$(update-alternatives --list java 2>/dev/null | grep -E "java-25|jdk-25" | head -1 || true)
    if [ -n "$JAVA25" ]; then
        sudo update-alternatives --set java "$JAVA25"
        JAVAC25=$(echo "$JAVA25" | sed 's|/bin/java||')/bin/javac
        [ -f "$JAVAC25" ] && sudo update-alternatives --set javac "$JAVAC25" 2>/dev/null || true
        success "Java 25 configurado como predeterminado: $JAVA25"
    fi
fi

JAVA_VER=$(java -version 2>&1 | head -1)
info "Java activo: $JAVA_VER"

JAVA_MAJOR=$(java -version 2>&1 | grep -oP '(?<=version ")[0-9]+' | head -1)
if [ -z "$JAVA_MAJOR" ]; then
    JAVA_MAJOR=$(java -version 2>&1 | grep -oP '[0-9]+\.[0-9]+' | head -1 | cut -d. -f1)
fi

if [ "$JAVA_MAJOR" -lt 25 ] 2>/dev/null; then
    error "Se requiere Java 25 o superior. Versión actual: $JAVA_VER\nInstala JDK 25 manualmente desde https://jdk.java.net/25/"
fi
success "Java $JAVA_MAJOR detectado."

# -----------------------------------------------------------------------------
# 4. Verificar Maven
# -----------------------------------------------------------------------------
info "Verificando Maven..."
MVN_VER=$(mvn -version 2>&1 | head -1)
success "Maven: $MVN_VER"

# -----------------------------------------------------------------------------
# 5. Verificar librerías nativas
# -----------------------------------------------------------------------------
info "Verificando librerías nativas..."

# jllama
if [ ! -f "${SCRIPT_DIR}/lib/llama/libjllama.so" ]; then
    error "Falta lib/llama/libjllama.so\nCompílala siguiendo las instrucciones en lib/llama/BUILD.md"
fi
success "libjllama.so encontrada."

# sherpa-onnx
SHERPA_DIR="${SCRIPT_DIR}/lib/sherpa-onnx/linux"
for lib in libsherpa-onnx-jni.so libonnxruntime.so; do
    if [ ! -f "${SHERPA_DIR}/${lib}" ]; then
        error "Falta ${SHERPA_DIR}/${lib}\nDescárgala desde https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.0"
    fi
done
success "Librerías sherpa-onnx encontradas."

# Verificar dependencias de las .so
info "Verificando dependencias de librerías nativas (ldd)..."
if ldd "${SCRIPT_DIR}/lib/llama/libjllama.so" 2>&1 | grep -q "not found"; then
    warn "Faltan dependencias en libjllama.so:"
    ldd "${SCRIPT_DIR}/lib/llama/libjllama.so" 2>&1 | grep "not found"
fi
if ldd "${SHERPA_DIR}/libsherpa-onnx-jni.so" 2>&1 | grep -q "not found"; then
    warn "Faltan dependencias en libsherpa-onnx-jni.so:"
    ldd "${SHERPA_DIR}/libsherpa-onnx-jni.so" 2>&1 | grep "not found"
fi

# -----------------------------------------------------------------------------
# 6. Instalar JARs de sherpa-onnx en el repositorio Maven local
# -----------------------------------------------------------------------------
info "Instalando JARs de sherpa-onnx en el repositorio Maven local..."

SHERPA_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-v1.13.0.jar"
SHERPA_NATIVE_WIN_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-native-lib-win-x64-v1.13.0.jar"
SHERPA_NATIVE_LINUX_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-native-linux-x64-1.13.0.jar"

# Instalar sherpa-onnx (API Java)
if [ -f "$SHERPA_JAR" ]; then
    mvn install:install-file -q \
        -Dfile="$SHERPA_JAR" \
        -DgroupId=com.k2fsa.sherpa \
        -DartifactId=sherpa-onnx \
        -Dversion=1.13.0 \
        -Dpackaging=jar
    success "sherpa-onnx-v1.13.0.jar instalado en Maven local."
else
    error "No se encontró $SHERPA_JAR"
fi

# Instalar sherpa-onnx-native-win-x64 (necesario para resolver el perfil, aunque no se use en Linux)
if [ -f "$SHERPA_NATIVE_WIN_JAR" ]; then
    mvn install:install-file -q \
        -Dfile="$SHERPA_NATIVE_WIN_JAR" \
        -DgroupId=com.k2fsa.sherpa \
        -DartifactId=sherpa-onnx-native-win-x64 \
        -Dversion=1.13.0 \
        -Dpackaging=jar
    success "sherpa-onnx-native-win-x64 instalado en Maven local."
fi

# Descargar sherpa-onnx-native-linux-x64 si no existe
if [ ! -f "$SHERPA_NATIVE_LINUX_JAR" ]; then
    info "Descargando sherpa-onnx-native-linux-x64-1.13.0.jar..."
    SHERPA_LINUX_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.0/sherpa-onnx-v1.13.0-linux-x64-jni.jar"
    wget -q --show-progress -O "$SHERPA_NATIVE_LINUX_JAR" "$SHERPA_LINUX_URL" || {
        warn "No se pudo descargar desde GitHub. Intentando crear JAR desde las .so locales..."
        # Crear un JAR con las .so locales como fallback
        TMPDIR_JAR=$(mktemp -d)
        mkdir -p "${TMPDIR_JAR}/linux-x86-64"
        cp "${SHERPA_DIR}"/*.so "${TMPDIR_JAR}/linux-x86-64/"
        jar cf "$SHERPA_NATIVE_LINUX_JAR" -C "$TMPDIR_JAR" .
        rm -rf "$TMPDIR_JAR"
        success "JAR nativo Linux creado desde librerías locales."
    }
fi

mvn install:install-file -q \
    -Dfile="$SHERPA_NATIVE_LINUX_JAR" \
    -DgroupId=com.k2fsa.sherpa \
    -DartifactId=sherpa-onnx-native-linux-x64 \
    -Dversion=1.13.0 \
    -Dpackaging=jar
success "sherpa-onnx-native-linux-x64 instalado en Maven local."

# -----------------------------------------------------------------------------
# 7. Copiar librerías nativas a /tmp
# -----------------------------------------------------------------------------
info "Copiando librerías nativas a /tmp..."
cp "${SHERPA_DIR}"/*.so /tmp/
cp "${SCRIPT_DIR}/lib/llama/libjllama.so" /tmp/libjllama.so

# El JAR de sherpa-onnx llama a System.loadLibrary("sherpa-onnx-jni") que busca
# "libsherpa-onnx-jni.so" en java.library.path. Crear symlink sin guiones también
# por si el JAR busca "sherpa_onnx_jni" (algunas versiones usan guión bajo).
ln -sf /tmp/libsherpa-onnx-jni.so /tmp/libsherpa_onnx_jni.so 2>/dev/null || true

success "Librerías copiadas a /tmp."

# -----------------------------------------------------------------------------
# 8. Configurar audio (PulseAudio / PipeWire / WSLg)
# -----------------------------------------------------------------------------
if [ -S "/mnt/wslg/PulseServer" ]; then
    export PULSE_SERVER="unix:/mnt/wslg/PulseServer"
    info "Audio: WSLg PulseAudio detectado → $PULSE_SERVER"
else
    info "Audio: usando servidor de audio del sistema."
fi

# -----------------------------------------------------------------------------
# 9. Compilar el proyecto
# -----------------------------------------------------------------------------
info "Compilando SoterIA con Maven..."
export LD_LIBRARY_PATH="/tmp:${LD_LIBRARY_PATH}"

mvn clean package -DskipTests -q \
    || error "La compilación falló. Revisa los errores de Maven arriba."

success "Compilación exitosa."

# -----------------------------------------------------------------------------
# 10. Lanzar la aplicación
# -----------------------------------------------------------------------------
echo ""
echo "============================================="
success "Todo listo. Iniciando SoterIA..."
echo "============================================="
echo ""

export LD_LIBRARY_PATH="/tmp:${LD_LIBRARY_PATH}"

mvn javafx:run \
    -Djava.library.path="/tmp:${SCRIPT_DIR}/lib/sherpa-onnx/linux" \
    -Dde.kherud.llama.lib.path=/tmp/libjllama.so
