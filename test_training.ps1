# Test script for Classification Training endpoint
# This script demonstrates how to call the training endpoint

Write-Host "Classification Training Test Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$baseUrl = "http://localhost:8080/api"
$transformerId = 1
$baselineImageId = 1
$maintenanceImageId = 2

Write-Host "Step 1: Verify transformer exists" -ForegroundColor Yellow
try {
    $transformer = Invoke-RestMethod -Uri "$baseUrl/transformers/$transformerId" -Method GET
    Write-Host "✓ Transformer found: $($transformer.transformerNo)" -ForegroundColor Green
} catch {
    Write-Host "✗ Transformer not found. Please create a transformer first." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Check baseline image" -ForegroundColor Yellow
try {
    $baselineImage = Invoke-RestMethod -Uri "$baseUrl/transformers/images/$baselineImageId/raw" -Method HEAD
    Write-Host "✓ Baseline image found (ID: $baselineImageId)" -ForegroundColor Green
} catch {
    Write-Host "✗ Baseline image not found. Please upload a baseline image first." -ForegroundColor Red
    Write-Host "Upload using: POST $baseUrl/transformers/$transformerId/images" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Step 3: Check maintenance image" -ForegroundColor Yellow
try {
    $maintenanceImage = Invoke-RestMethod -Uri "$baseUrl/transformers/images/$maintenanceImageId/raw" -Method HEAD
    Write-Host "✓ Maintenance image found (ID: $maintenanceImageId)" -ForegroundColor Green
} catch {
    Write-Host "✗ Maintenance image not found. Please upload a maintenance image first." -ForegroundColor Red
    Write-Host "Upload using: POST $baseUrl/transformers/$transformerId/images?inspectionId=X" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Step 4: Check anomaly detection results" -ForegroundColor Yellow
try {
    $anomalyResults = Invoke-RestMethod -Uri "$baseUrl/transformers/images/$maintenanceImageId/fault-regions" -Method GET
    if ($anomalyResults.Count -eq 0) {
        Write-Host "✗ No fault regions found for maintenance image." -ForegroundColor Red
        Write-Host "Anomaly detection must run first (happens automatically on maintenance image upload)." -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✓ Found $($anomalyResults.Count) fault regions" -ForegroundColor Green
} catch {
    Write-Host "✗ Could not retrieve anomaly results." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 5: Call training endpoint" -ForegroundColor Yellow
Write-Host "Sending training request..." -ForegroundColor Cyan

$trainingRequest = @{
    transformerId = $transformerId
    baselineImageId = $baselineImageId
    maintenanceImageId = $maintenanceImageId
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/transformers/$transformerId/train" `
                                  -Method POST `
                                  -Body $trainingRequest `
                                  -ContentType "application/json"
    
    Write-Host ""
    Write-Host "✓ Training completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Training Results:" -ForegroundColor Cyan
    Write-Host "  Status:      $($response.status)" -ForegroundColor White
    Write-Host "  Message:     $($response.message)" -ForegroundColor White
    Write-Host "  Config ID:   $($response.configId)" -ForegroundColor White
    Write-Host "  Config Name: $($response.configName)" -ForegroundColor White
    Write-Host "  Trained At:  $($response.trainedAt)" -ForegroundColor White
    Write-Host ""
    Write-Host "The new configuration is now active in the database!" -ForegroundColor Green
    
} catch {
    Write-Host ""
    Write-Host "✗ Training failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        $errorDetails = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Host ""
        Write-Host "Details:" -ForegroundColor Yellow
        Write-Host "  Message: $($errorDetails.message)" -ForegroundColor White
        Write-Host "  Status:  $($errorDetails.status)" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  1. Flask API server not running (should be on port 5000)" -ForegroundColor White
    Write-Host "  2. Flask API missing /update-config endpoint" -ForegroundColor White
    Write-Host "  3. Images don't belong to the specified transformer" -ForegroundColor White
    Write-Host "  4. No anomaly detection results for maintenance image" -ForegroundColor White
    Write-Host "  5. Invalid image IDs" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "Step 6: Verify new configuration" -ForegroundColor Yellow
# Note: Would need a config endpoint to verify, but for now we just confirm training worked
Write-Host "✓ Training completed. Check database for new configuration." -ForegroundColor Green
Write-Host ""
Write-Host "Query to verify:" -ForegroundColor Cyan
Write-Host "  SELECT * FROM anomaly_detection_config WHERE is_active = true;" -ForegroundColor White
Write-Host ""
