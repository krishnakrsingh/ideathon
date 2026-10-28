# SwasthAI Model Downloader & Push Utility
# Downloads 100% on-device LiteRT-LM models for Gemma 4 E2B and EmbeddingGemma 2,
# then pushes them to your connected Android phone via ADB.

param(
    [switch]$PushToDevice,
    [string]$OutputDir = "./models"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   SwasthAI: 100% On-Device LiteRT-LM Model Setup Utility" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$GenerativeModelName = "gemma-4-E2B-it.litertlm"
# Official/Kaggle LiteRT-LM model weight target
$GenerativeModelUrl = "https://huggingface.co/google/gemma-4-E2B-it-litertlm/resolve/main/gemma-4-E2B-it.litertlm"
$GenerativeModelDest = Join-Path $OutputDir $GenerativeModelName

$EmbeddingModelName = "embeddinggemma-2-text-270m.litertlm"
$EmbeddingModelUrl = "https://huggingface.co/google/embeddinggemma-2-text-270m-litertlm/resolve/main/embeddinggemma-2-text-270m.litertlm"
$EmbeddingModelDest = Join-Path $OutputDir $EmbeddingModelName

# 1. Download Gemma 4 E2B LiteRT-LM
if (-not (Test-Path $GenerativeModelDest) -or ((Get-Item $GenerativeModelDest).Length -lt 100MB)) {
    Write-Host "`n[1/2] Downloading Google Gemma 4 E2B IT LiteRT-LM ($GenerativeModelName)..." -ForegroundColor Yellow
    try {
        curl.exe -L -C - -o $GenerativeModelDest $GenerativeModelUrl
        Write-Host "Done: $GenerativeModelDest" -ForegroundColor Green
    } catch {
        Write-Host "Download failed or direct URL not reachable. You can manually place $GenerativeModelName into $OutputDir" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n[1/2] Generative LiteRT-LM model already present: $GenerativeModelDest" -ForegroundColor Green
}

# 2. Download EmbeddingGemma 2 LiteRT-LM
if (-not (Test-Path $EmbeddingModelDest) -or ((Get-Item $EmbeddingModelDest).Length -lt 50MB)) {
    Write-Host "`n[2/2] Downloading EmbeddingGemma 2 (270M) LiteRT-LM ($EmbeddingModelName)..." -ForegroundColor Yellow
    try {
        curl.exe -L -C - -o $EmbeddingModelDest $EmbeddingModelUrl
        Write-Host "Done: $EmbeddingModelDest" -ForegroundColor Green
    } catch {
        Write-Host "Download failed or direct URL not reachable. You can manually place $EmbeddingModelName into $OutputDir" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n[2/2] Embedding LiteRT-LM model already present: $EmbeddingModelDest" -ForegroundColor Green
}

# 3. Optional ADB Push to connected phone
if ($PushToDevice) {
    Write-Host "`nChecking ADB connected devices..." -ForegroundColor Cyan
    $devices = adb devices
    Write-Host $devices

    Write-Host "Pushing models to /sdcard/Download/ on connected phone..." -ForegroundColor Yellow
    if (Test-Path $GenerativeModelDest) {
        adb push $GenerativeModelDest /sdcard/Download/
    }
    if (Test-Path $EmbeddingModelDest) {
        adb push $EmbeddingModelDest /sdcard/Download/
    }
    Write-Host "Push complete! Models are in /sdcard/Download/ and ready for offline inference." -ForegroundColor Green
} else {
    Write-Host "`nTo push directly to your connected Android phone, run:" -ForegroundColor Cyan
    Write-Host "  .\download_models.ps1 -PushToDevice" -ForegroundColor White
    Write-Host "  (Or manually: adb push $OutputDir/*.litertlm /sdcard/Download/)" -ForegroundColor Gray
}

Write-Host "`nSwasthAI Model Setup Ready." -ForegroundColor Green
