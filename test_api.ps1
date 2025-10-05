# PowerShell script to test the transformer API and anomaly detection

Write-Host "=== Testing Transformer API and Anomaly Detection ===" -ForegroundColor Green

# 1. Create a transformer
Write-Host "`n1. Creating a transformer..." -ForegroundColor Yellow
$transformerData = @{
    transformerNo = "T001"
    poleNo = "P001"
    region = "Test Region"
    transformerType = "Distribution"
} | ConvertTo-Json

$transformer = Invoke-RestMethod -Uri "http://localhost:8080/api/transformers" -Method POST -Body $transformerData -ContentType "application/json"
Write-Host "Created transformer with ID: $($transformer.id)" -ForegroundColor Green

# 2. Create an inspection
Write-Host "`n2. Creating an inspection..." -ForegroundColor Yellow
$inspectionData = @{
    title = "Test Inspection"
    inspector = "Test Inspector"
    notes = "Test inspection for anomaly detection"
} | ConvertTo-Json

$inspection = Invoke-RestMethod -Uri "http://localhost:8080/api/transformers/$($transformer.id)/inspections" -Method POST -Body $inspectionData -ContentType "application/json"
Write-Host "Created inspection with ID: $($inspection.id)" -ForegroundColor Green

Write-Host "`n3. Next steps:" -ForegroundColor Cyan
Write-Host "   - Upload a baseline image to transformer ID: $($transformer.id)"
Write-Host "   - Upload a maintenance image to trigger anomaly detection"
Write-Host "   - Use inspection ID: $($inspection.id) for maintenance images"
Write-Host "`nTransformer ID: $($transformer.id)"
Write-Host "Inspection ID: $($inspection.id)"
