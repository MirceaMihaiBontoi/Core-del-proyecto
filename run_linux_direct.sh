#!/bin/sh
# SoterIA Linux Direct Launcher
# Runs Java directly without Maven

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Copy libraries to /tmp
echo "Copying native libraries to /tmp..."
cp "${SCRIPT_DIR}/lib/sherpa-onnx/linux/"*.so /tmp/ 2>/dev/null || true
cp "${SCRIPT_DIR}/lib/llama/jllama.so" /tmp/libjllama.so 2>/dev/null || true

# Set LD_LIBRARY_PATH
export LD_LIBRARY_PATH="/tmp:${LD_LIBRARY_PATH}"

# Configure audio subsystem (PulseAudio/PipeWire)
# WSL2 with WSLg provides a PulseAudio socket at /mnt/wslg/PulseServer
if [ -S "/mnt/wslg/PulseServer" ]; then
    export PULSE_SERVER="unix:/mnt/wslg/PulseServer"
    echo "Audio: WSLg PulseAudio socket detected"
elif [ -z "$PULSE_SERVER" ]; then
    # Native Linux — assume local PulseAudio/PipeWire is running
    echo "Audio: Using system default audio server"
fi

echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"
echo "PULSE_SERVER=${PULSE_SERVER}"
echo "Starting SoterIA with direct Java execution..."

# Build classpath and module path
CP="${SCRIPT_DIR}/target/classes"
MODULE_PATH=""

if [ -d "${SCRIPT_DIR}/target/lib" ]; then
    for jar in "${SCRIPT_DIR}"/target/lib/*.jar; do
        if [ -f "$jar" ]; then
            # Filter out Windows and Mac specific jars
            if echo "$jar" | grep -qE -- "-win|-mac"; then
                continue
            fi
            
            # Only JavaFX and Ikonli need to be on the module path. The rest go to classpath.
            if echo "$jar" | grep -qE -- "javafx.*\.jar|ikonli.*\.jar"; then
                if [ -z "$MODULE_PATH" ]; then
                    MODULE_PATH="$jar"
                else
                    MODULE_PATH="${MODULE_PATH}:$jar"
                fi
            else
                CP="${CP}:${jar}"
            fi
        fi
    done
else
    echo "Warning: target/lib directory not found. Please run 'mvn clean package' first."
fi

# Add local libs if any
for jar in "${SCRIPT_DIR}"/lib/*.jar; do
    if [ -f "$jar" ]; then
        CP="${CP}:${jar}"
    fi
done

# Run Java
java \
    --module-path "${MODULE_PATH}" \
    --add-modules=javafx.controls,javafx.fxml,jdk.incubator.vector,org.kordamp.ikonli.core,org.kordamp.ikonli.javafx,org.kordamp.ikonli.material2 \
    --enable-native-access=ALL-UNNAMED \
    -Djava.library.path=/tmp \
    -Dde.kherud.llama.lib.path=/tmp/libjllama.so \
    -cp "${CP}" \
    com.soteria.ui.MainApp
