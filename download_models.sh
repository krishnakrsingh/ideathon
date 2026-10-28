#!/usr/bin/env bash
# SwasthAI Model Downloader & Push Utility for Linux / macOS / WSL
set -e

OUTPUT_DIR="./models"
mkdir -p "$OUTPUT_DIR"

GEN_NAME="gemma-4-E2B-it.litertlm"
GEN_URL="https://huggingface.co/google/gemma-4-E2B-it-litertlm/resolve/main/gemma-4-E2B-it.litertlm"
GEN_DEST="$OUTPUT_DIR/$GEN_NAME"

EMB_NAME="embeddinggemma-2-text-270m.litertlm"
EMB_URL="https://huggingface.co/google/embeddinggemma-2-text-270m-litertlm/resolve/main/embeddinggemma-2-text-270m.litertlm"
EMB_DEST="$OUTPUT_DIR/$EMB_NAME"

echo "=== SwasthAI: 100% On-Device LiteRT-LM Model Downloader ==="

if [ ! -f "$GEN_DEST" ]; then
    echo "Downloading Google Gemma 4 E2B IT LiteRT-LM ($GEN_NAME)..."
    curl -L -C - -o "$GEN_DEST" "$GEN_URL" || echo "Direct download failed. Place $GEN_NAME manually in $OUTPUT_DIR"
else
    echo "Gemma 4 E2B already downloaded: $GEN_DEST"
fi

if [ ! -f "$EMB_DEST" ]; then
    echo "Downloading EmbeddingGemma 2 LiteRT-LM ($EMB_NAME)..."
    curl -L -C - -o "$EMB_DEST" "$EMB_URL" || echo "Direct download failed. Place $EMB_NAME manually in $OUTPUT_DIR"
else
    echo "Embedding model already downloaded: $EMB_DEST"
fi

if [ "$1" == "--push" ]; then
    echo "Pushing models to connected device via adb..."
    adb devices
    [ -f "$GEN_DEST" ] && adb push "$GEN_DEST" /sdcard/Download/
    [ -f "$EMB_DEST" ] && adb push "$EMB_DEST" /sdcard/Download/
    echo "Done pushing to /sdcard/Download/!"
fi

echo "Done! Models ready."
