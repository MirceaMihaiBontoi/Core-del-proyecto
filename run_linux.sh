#!/bin/sh
# SoterIA Linux Launcher
# Sets up the environment and runs the application

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Copy libraries to /tmp (which is in java.library.path)
echo "Copying native libraries to /tmp..."
cp "${SCRIPT_DIR}/lib/sherpa-onnx/linux/"*.so /tmp/ 2>/dev/null || true

# Set LD_LIBRARY_PATH to include /tmp so dlopen() can find dependencies
export LD_LIBRARY_PATH="/tmp:${LD_LIBRARY_PATH}"

echo "LD_LIBRARY_PATH=${LD_LIBRARY_PATH}"
echo "Starting SoterIA..."

# Run Maven with JavaFX
mvn javafx:run
