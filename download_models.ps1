# SwasthAI Model Downloader & Push Utility
# Downloads 100% on-device GGUF models for Gemma 3 1B and LFM2.5 Embedding,
# then pushes them to your connected Android phone via ADB.

param(
    [switch]$PushToDevice,
    [string]$OutputDir = "./models"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   SwasthAI: 100% On-Device Model Setup Utility" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$GenerativeModelName = "gemma-3-1b-it-Q4_K_M.gguf"
$GenerativeModelUrl = "https://huggingface.co/google/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf"
$GenerativeModelDest = Join-Path $OutputDir $GenerativeModelName

$EmbeddingModelName = "LFM2.5-Embedding-350M-GGUF-Q4_K_M.gguf"
$EmbeddingModelUrl = "https://huggingface.co/LiquidAI/LFM2.5-Embedding-350M-GGUF/resolve/main/LFM2.5-Embedding-350M-GGUF-Q4_K_M.gguf"
$EmbeddingModelDest = Join-Path $OutputDir $EmbeddingModelName

# 1. Download Gemma 3 1B IT GGUF
if (-not (Test-Path $GenerativeModelDest) -or ((Get-Item $GenerativeModelDest).Length -lt 700MB)) {
    Write-Host "`n[1/2] Downloading Google Gemma 3 1B IT (Q4_K_M, ~769 MB)..." -ForegroundColor Yellow
    curl.exe -L -C - -o $GenerativeModelDest $GenerativeModelUrl
    Write-Host "Done: $GenerativeModelDest" -ForegroundColor Green
} else {
    Write-Host "`n[1/2] Generative model already present: $GenerativeModelDest" -ForegroundColor Green
}

# 2. Download LFM2.5 Embedding GGUF
if (-not (Test-Path $EmbeddingModelDest) -or ((Get-Item $EmbeddingModelDest).Length -lt 200MB)) {
    Write-Host "`n[2/2] Downloading Liquid AI LFM2.5 Embedding 350M (~235 MB)..." -ForegroundColor Yellow
    curl.exe -L -C - -o $EmbeddingModelDest $EmbeddingModelUrl
    Write-Host "Done: $EmbeddingModelDest" -ForegroundColor Green
} else {
    Write-Host "`n[2/2] Embedding model already present: $EmbeddingModelDest" -ForegroundColor Green
}

# 3. Optional ADB Push to connected phone
if ($PushToDevice) {
    Write-Host "`nChecking ADB connected devices..." -ForegroundColor Cyan
    $devices = adb devices
    Write-Host $devices

    Write-Host "Pushing models to /sdcard/Download/ on connected phone..." -ForegroundColor Yellow
    adb push $GenerativeModelDest /sdcard/Download/
    adb push $EmbeddingModelDest /sdcard/Download/
    Write-Host "Push complete! Models are in /sdcard/Download/ and ready for offline inference." -ForegroundColor Green
} else {
    Write-Host "`nTo push directly to your connected Android phone, run:" -ForegroundColor Cyan
    Write-Host "  .\download_models.ps1 -PushToDevice" -ForegroundColor White
    Write-Host "  (Or manually: adb push $OutputDir/*.gguf /sdcard/Download/)" -ForegroundColor Gray
}

Write-Host "`nSwasthAI Model Setup Ready." -ForegroundColor Green
