[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$ResetData
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$namespace = "diabetes-monitor"

function Resolve-Tool {
    param(
        [string]$Name,
        [string[]]$Candidates
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    foreach ($candidate in $Candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    throw "Cannot find $Name. Start Docker Desktop and enable Kubernetes first."
}

$docker = Resolve-Tool "docker" @(
    "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\docker.exe",
    "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
)
$kubectl = Resolve-Tool "kubectl" @(
    "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin\kubectl.exe",
    "C:\Program Files\Docker\Docker\resources\bin\kubectl.exe"
)

& $kubectl cluster-info | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Kubernetes is not ready. Enable it in Docker Desktop first."
}

if ($ResetData) {
    Write-Host "Deleting namespace $namespace and all persistent data..."
    & $kubectl delete namespace $namespace --ignore-not-found
    & $kubectl wait --for=delete "namespace/$namespace" --timeout=120s 2>$null
}

$mysqlPassword = if ($env:MYSQL_ROOT_PASSWORD) {
    $env:MYSQL_ROOT_PASSWORD
} else {
    "mysql-" + [Guid]::NewGuid().ToString("N")
}

$redisPassword = if ($env:REDIS_PASSWORD) {
    $env:REDIS_PASSWORD
} else {
    "redis-" + [Guid]::NewGuid().ToString("N")
}

$jwtSecret = if ($env:JWT_SECRET) {
    $env:JWT_SECRET
} else {
    "jwt-" + [Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N")
}

if (-not $env:SPRING_AI_DASHSCOPE_API_KEY) {
    throw "Set SPRING_AI_DASHSCOPE_API_KEY before deployment."
}
$dashScopeApiKey = $env:SPRING_AI_DASHSCOPE_API_KEY

if (-not $SkipBuild) {
    Write-Host "Building local images..."
    & $docker build -t diabetes-monitor-backend:local "$repoRoot\diabetes-backend"
    if ($LASTEXITCODE -ne 0) { throw "Backend image build failed." }

    & $docker build -t diabetes-monitor-frontend:local "$repoRoot\diabetes-frontend"
    if ($LASTEXITCODE -ne 0) { throw "Frontend image build failed." }
}

Write-Host "Applying namespace and configuration..."
& $kubectl apply -f "$PSScriptRoot\namespace.yaml"

$secretArgs = @(
    "create", "secret", "generic", "diabetes-secrets",
    "--namespace", $namespace,
    "--from-literal=MYSQL_ROOT_PASSWORD=$mysqlPassword",
    "--from-literal=DB_PASSWORD=$mysqlPassword",
    "--from-literal=REDIS_PASSWORD=$redisPassword",
    "--from-literal=JWT_SECRET=$jwtSecret",
    "--from-literal=SPRING_AI_DASHSCOPE_API_KEY=$dashScopeApiKey",
    "--dry-run=client",
    "-o", "yaml"
)
$secretYaml = & $kubectl @secretArgs
$secretYaml | & $kubectl apply -f -

$configMapYaml = & $kubectl create configmap mysql-init `
    --namespace $namespace `
    --from-file="init.sql=$repoRoot\sql\init.sql" `
    --dry-run=client `
    -o yaml
$configMapYaml | & $kubectl apply -f -

Write-Host "Applying application resources..."
& $kubectl apply -k $PSScriptRoot

Write-Host "Waiting for MySQL and Redis..."
& $kubectl rollout status statefulset/mysql -n $namespace --timeout=300s
& $kubectl rollout status statefulset/redis -n $namespace --timeout=180s

Write-Host "Waiting for backend and frontend..."
& $kubectl rollout status deployment/diabetes-backend -n $namespace --timeout=300s
& $kubectl rollout status deployment/diabetes-frontend -n $namespace --timeout=180s

Write-Host ""
& $kubectl get pods,svc,pvc -n $namespace
Write-Host ""
Write-Host "Frontend: http://localhost:30080"
Write-Host "Management health: kubectl port-forward -n $namespace svc/diabetes-backend 9090:9090"
