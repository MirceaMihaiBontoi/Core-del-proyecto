#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

SHERPA_LIB_PATH="${SCRIPT_DIR}/lib/sherpa-onnx/linux"
LLAMA_LIB_PATH="${SCRIPT_DIR}/lib/llama"
SHERPA_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-v1.13.0.jar"

rm -f /tmp/libonnxruntime.so /tmp/libjllama.so /tmp/libsherpa-onnx-*.so 2>/dev/null || true
SHERPA_NATIVE_LINUX_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-native-linux-x64-1.13.0.jar"
SHERPA_NATIVE_WIN_JAR="${SCRIPT_DIR}/lib/sherpa-onnx/sherpa-onnx-native-lib-win-x64-v1.13.0.jar"

mkdir -p "${SCRIPT_DIR}/logs/voice" "${SCRIPT_DIR}/logs/raw_llm" "${SCRIPT_DIR}/logs/raw_classifier"

mvn install:install-file -q \
    -Dfile="$SHERPA_JAR" \
    -DgroupId=com.k2fsa.sherpa \
    -DartifactId=sherpa-onnx \
    -Dversion=1.13.0 \
    -Dpackaging=jar

if [ -f "$SHERPA_NATIVE_WIN_JAR" ]; then
    mvn install:install-file -q \
        -Dfile="$SHERPA_NATIVE_WIN_JAR" \
        -DgroupId=com.k2fsa.sherpa \
        -DartifactId=sherpa-onnx-native-win-x64 \
        -Dversion=1.13.0 \
        -Dpackaging=jar
fi

mvn install:install-file -q \
    -Dfile="$SHERPA_NATIVE_LINUX_JAR" \
    -DgroupId=com.k2fsa.sherpa \
    -DartifactId=sherpa-onnx-native-linux-x64 \
    -Dversion=1.13.0 \
    -Dpackaging=jar

export LD_LIBRARY_PATH="${SHERPA_LIB_PATH}:${LD_LIBRARY_PATH}"

exec mvn javafx:run \
    -Djava.library.path="${SHERPA_LIB_PATH}" \
    -Dde.kherud.llama.lib.path="${LLAMA_LIB_PATH}"
