# jllama Build Instructions

This directory contains platform-specific jllama native libraries for SoterIA.

## Current Files

- `jllama.dll` - Windows x64 native library
- `jllama.so` - Linux x64 native library (to be added)

## Building jllama for Linux

### Prerequisites

- CMake 3.15+
- GCC/G++ 9+ with C++17 support
- Git

### Build Steps

```bash
# Clone the SoterIA fork
git clone https://github.com/MirceaMihaiBontoi/java-llama.cpp.git
cd java-llama.cpp

# Create build directory
mkdir build && cd build

# Configure CMake (CPU-only, no CUDA/Metal)
cmake .. -DCMAKE_BUILD_TYPE=Release \
         -DLLAMA_NATIVE_ARCH=OFF \
         -DLLAMA_CUDA=OFF \
         -DLLAMA_METAL=OFF

# Build
make -j$(nproc)

# The output will be libjllama.so
```

### Verification

```bash
# Check JNI symbols
nm -D libjllama.so | grep Java_de_kherud_llama

# Check dependencies
ldd libjllama.so
```

### Installation

Copy the compiled `libjllama.so` to this directory:

```bash
cp libjllama.so /path/to/soteria/lib/llama/jllama.so
```

## Notes

- The jllama library must be built from the MirceaMihaiBontoi/java-llama.cpp fork
- This fork is specifically configured for SoterIA's requirements
- The Java API is provided by the `de.kherud:llama` Maven dependency
- The native library must match the fork's llama.cpp version
