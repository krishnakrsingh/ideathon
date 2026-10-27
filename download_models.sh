#!/usr/bin/env bash
# SwasthAI Model Downloader & Push Utility for Linux / macOS / WSL
set -e

OUTPUT_DIR="./models"
mkdir -p "$OUTPUT_DIR"

GEN_NAME="gemma-3-1b-it-Q4_K_M.gguf"
GEN_URL="https://huggingface.co/google/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf"
GEN_DEST="$OUTPUT_DIR/$GEN_NAME"

EMB_NAME="LFM2.5-Embedding-350M-GGUF-Q4_K_M.gguf"
EMB_URL="https://huggingface.co/LiquidAI/LFM2.5-Embedding-350M-GGUF/resolve/main/LFM2.5-Embedding-350M-GGUF-Q4_K_M.gguf"
EMB_DEST="$OUTPUT_DIR/$EMB_NAME"

echo "=== SwasthAI: 100% On-Device Model Downloader ==="

if [ ! -f "$GEN_DEST" ]; then
    echo "Downloading Google Gemma 3 1B IT (769 MB)..."
    curl -L -C - -o "$GEN_DEST" "$GEN_URL"
else
    echo "Gemma 3 1B already downloaded: $GEN_DEST"
fi

if [ ! -f "$EMB_DEST" ]; then
    echo "Downloading LFM2.5 Embedding 350M (235 MB)..."
    curl -L -C - -o "$EMB_DEST" "$EMB_URL"
else
    echo "Embedding model already downloaded: $EMB_DEST"
fi

if [ "$1" == "--push" ]; then
    echo "Pushing models to connected device via adb..."
    adb devices
    adb push "$GEN_DEST" /sdcard/Download/
    adb push "$EMB_DEST" /sdcard/Download/
    echo "Done pushing to /sdcard/Download/!"
fi

echo "Done! Models ready."
