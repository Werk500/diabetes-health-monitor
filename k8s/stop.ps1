[CmdletBinding()]
param(
    [switch]$DeleteData
)

$ErrorActionPreference = "Stop"
$kubectl = Get-Command kubectl -ErrorAction SilentlyContinue
if (-not $kubectl) {
    $candidate = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\kubectl.exe"
    if (-not (Test-Path -LiteralPath $candidate)) {
        throw "kubectl was not found."
    }
    $kubectlExe = $candidate
} else {
    $kubectlExe = $kubectl.Source
}

if ($DeleteData) {
    Write-Host "Deleting namespace and persistent data..."
    & $kubectlExe delete namespace diabetes-monitor --ignore-not-found
} else {
    Write-Host "Deleting application resources but keeping PVCs..."
    & $kubectlExe delete -k $PSScriptRoot --ignore-not-found
}
