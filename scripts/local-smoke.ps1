[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8088",
    [string]$FrontendUrl = "http://127.0.0.1:5173",
    [switch]$SkipAi,
    [switch]$SkipExport
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$script:Passed = 0
$script:CreatedRecords = [System.Collections.Generic.List[object]]::new()
$script:ExportTaskIds = [System.Collections.Generic.List[long]]::new()

function Write-Pass {
    param([string]$Message)
    $script:Passed++
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Get-ResponseJson {
    param([Microsoft.PowerShell.Commands.WebResponseObject]$Response)
    return $Response.Content | ConvertFrom-Json
}

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST", "PUT", "DELETE")]
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body
    )

    $request = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        UseBasicParsing = $true
        TimeoutSec = 90
    }

    if ($Token) {
        $request.Headers = @{ Authorization = "Bearer $Token" }
    }

    if ($null -ne $Body) {
        $request.ContentType = "application/json; charset=utf-8"
        $request.Body = if ($Body -is [string]) {
            $Body
        } else {
            $Body | ConvertTo-Json -Depth 8 -Compress
        }
    }

    $response = Invoke-WebRequest @request
    $json = Get-ResponseJson $response

    if ($json.code -ne 200) {
        throw "$Method $Path returned code=$($json.code), msg=$($json.msg)"
    }

    return $json.data
}

function Encrypt-Password {
    param(
        [System.Security.Cryptography.RSA]$Rsa,
        [string]$Text
    )

    $encrypted = $Rsa.Encrypt(
        [Text.Encoding]::UTF8.GetBytes($Text),
        [System.Security.Cryptography.RSAEncryptionPadding]::Pkcs1
    )
    return [Convert]::ToBase64String($encrypted)
}

function New-LoginSession {
    param(
        [string]$Username,
        [string]$Password
    )

    $publicKeyResponse = Invoke-RestMethod -Uri "$BaseUrl/api/user/public-key" -Method Get
    if ($publicKeyResponse.code -ne 200) {
        throw "Cannot load RSA public key."
    }

    $rsa = [System.Security.Cryptography.RSA]::Create()
    $read = 0
    $rsa.ImportSubjectPublicKeyInfo(
        [Convert]::FromBase64String($publicKeyResponse.data),
        [ref]$read
    )

    $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $body = @{
        username = $Username
        password = Encrypt-Password -Rsa $rsa -Text $Password
    } | ConvertTo-Json -Compress

    $response = Invoke-WebRequest `
        -UseBasicParsing `
        -WebSession $session `
        -Method Post `
        -Uri "$BaseUrl/api/user/login" `
        -ContentType "application/json; charset=utf-8" `
        -Body $body `
        -TimeoutSec 30

    $json = Get-ResponseJson $response
    if ($json.code -ne 200) {
        throw "Login failed for ${Username}: $($json.msg)"
    }

    $refreshCookie = $session.Cookies.GetAllCookies() |
        Where-Object Name -eq "refresh_token" |
        Select-Object -First 1
    if (-not $refreshCookie) {
        throw "Login did not set the refresh cookie."
    }

    return [pscustomobject]@{
        Token = [string]$json.data.accessToken
        UserId = [int]$json.data.user.id
        Session = $session
    }
}

function Assert-Status {
    param(
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [string]$Token
    )

    $parameters = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        UseBasicParsing = $true
        SkipHttpErrorCheck = $true
        TimeoutSec = 30
    }
    if ($Token) {
        $parameters.Headers = @{ Authorization = "Bearer $Token" }
    }

    $response = Invoke-WebRequest @parameters
    if ($response.StatusCode -ne $ExpectedStatus) {
        throw "$Method $Path expected HTTP $ExpectedStatus, got $($response.StatusCode)."
    }
}

function Add-HealthRecord {
    param(
        [string]$Token,
        [int]$UserId,
        [string]$Type,
        [object]$Body,
        [string]$Remark
    )

    Invoke-Api -Method POST -Path "/api/record/$Type" -Token $Token -Body $Body | Out-Null

    $records = Invoke-Api -Method GET -Path "/api/record/$Type/list/$UserId" -Token $Token
    $record = $records | Where-Object { $_.remark -eq $Remark } | Select-Object -First 1
    if (-not $record) {
        throw "Created $Type record cannot be found by remark."
    }

    $deletePrefix = switch ($Type) {
        "body" { "/api/record/body/" }
        "bloodSugar" { "/api/record/bloodSugar/" }
        "diet" { "/api/record/diet/" }
        "exercise" { "/api/record/exercise/" }
        default { throw "Unsupported health record type: $Type" }
    }

    $script:CreatedRecords.Add([pscustomobject]@{
        Token = $Token
        Path = "$deletePrefix$($record.id)"
        Type = $Type
    })

    return $record
}

function Wait-ExportTask {
    param(
        [string]$Token,
        [long]$TaskId
    )

    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        $task = Invoke-Api -Method GET -Path "/api/export/tasks/$TaskId" -Token $Token
        if ($task.status -eq "SUCCESS" -or $task.status -eq "FAILED") {
            return $task
        }
        Start-Sleep -Seconds 1
    }

    throw "Export task $TaskId did not finish within 45 seconds."
}

try {
    Write-Info "Checking frontend and API proxy..."
    $frontend = Invoke-WebRequest -UseBasicParsing -Uri "$FrontendUrl/" -TimeoutSec 10
    if ($frontend.StatusCode -ne 200) {
        throw "Frontend returned HTTP $($frontend.StatusCode)."
    }
    $proxyResponse = Invoke-RestMethod -Uri "$FrontendUrl/api/user/public-key" -Method Get
    if ($proxyResponse.code -ne 200) {
        throw "Frontend API proxy is not reachable."
    }
    Write-Pass "frontend page and Vite API proxy"

    $userLogin = New-LoginSession -Username "test001" -Password "123456"
    $token = $userLogin.Token
    $userId = $userLogin.UserId
    Write-Pass "user login and HttpOnly refresh cookie"

    $oldCookie = ($userLogin.Session.Cookies.GetAllCookies() |
        Where-Object Name -eq "refresh_token" |
        Select-Object -First 1).Value
    $refreshResponse = Invoke-WebRequest `
        -UseBasicParsing `
        -WebSession $userLogin.Session `
        -Method Post `
        -Uri "$BaseUrl/api/user/refresh" `
        -TimeoutSec 30
    $refreshJson = Get-ResponseJson $refreshResponse
    if ($refreshJson.code -ne 200 -or -not $refreshJson.data.accessToken) {
        throw "Refresh token endpoint did not return a new access token."
    }
    $newCookie = ($userLogin.Session.Cookies.GetAllCookies() |
        Where-Object Name -eq "refresh_token" |
        Select-Object -First 1).Value
    if ($newCookie -eq $oldCookie) {
        throw "Refresh token was not rotated."
    }
    $token = [string]$refreshJson.data.accessToken
    Write-Pass "access token refresh and refresh-token rotation"

    $profile = Invoke-Api -Method GET -Path "/api/user/info/$userId" -Token $token
    if ([int]$profile.id -ne $userId) {
        throw "Authenticated user endpoint returned another user."
    }
    Assert-Status -Method GET -Path "/api/admin/stats" -ExpectedStatus 403 -Token $token
    Assert-Status -Method GET -Path "/api/user/info/$userId" -ExpectedStatus 401
    Write-Pass "self profile, unauthenticated rejection and user/admin separation"

    $suffix = Get-Date -Format "yyyyMMddHHmmssfff"
    $bodyRemark = "e2e-body-$suffix"
    $bloodRemark = "e2e-blood-$suffix"
    $dietRemark = "e2e-diet-$suffix"
    $exerciseRemark = "e2e-exercise-$suffix"
    $today = Get-Date -Format "yyyy-MM-dd"
    $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

    Add-HealthRecord -Token $token -UserId $userId -Type "body" -Remark $bodyRemark -Body @{
        weight = 65.2
        bmi = 22.1
        bodyFat = 20.0
        systolicPressure = 120
        diastolicPressure = 80
        heartRate = 72
        waistline = 80.0
        recordDate = $today
        remark = $bodyRemark
    } | Out-Null

    Add-HealthRecord -Token $token -UserId $userId -Type "bloodSugar" -Remark $bloodRemark -Body @{
        bloodSugar = 5.6
        measureType = 1
        measureTime = $now
        remark = $bloodRemark
    } | Out-Null

    Add-HealthRecord -Token $token -UserId $userId -Type "diet" -Remark $dietRemark -Body @{
        foodName = "E2E燕麦"
        mealType = 1
        calories = 250.0
        carbs = 30.0
        protein = 10.0
        fat = 5.0
        fiber = 5.0
        portion = 200.0
        eatTime = $now
        remark = $dietRemark
    } | Out-Null

    $exerciseTypes = Invoke-Api -Method GET -Path "/api/record/exerciseType/list" -Token $token
    if (-not $exerciseTypes -or $exerciseTypes.Count -lt 1) {
        throw "No enabled exercise type is available."
    }
    Add-HealthRecord -Token $token -UserId $userId -Type "exercise" -Remark $exerciseRemark -Body @{
        exerciseTypeId = [int]$exerciseTypes[0].id
        durationMinutes = 30
        caloriesBurned = 150.0
        heartRateAvg = 100
        exerciseDate = $today
        remark = $exerciseRemark
    } | Out-Null
    Write-Pass "body, blood sugar, diet and exercise create/query flows"

    Invoke-Api -Method GET -Path "/api/dashboard/$userId" -Token $token | Out-Null
    Invoke-Api -Method GET -Path "/api/record/bloodSugar/trend/${userId}?days=14" -Token $token | Out-Null
    Invoke-Api -Method GET -Path "/api/record/bloodSugar/metrics/${userId}?days=14" -Token $token | Out-Null
    Invoke-Api -Method GET -Path "/api/record/bloodSugar/daily/${userId}?days=14" -Token $token | Out-Null
    Invoke-Api -Method POST -Path "/api/record/bloodSugar/daily/backfill/$userId" -Token $token | Out-Null
    Invoke-Api -Method GET -Path "/api/record/diet/stats/$userId" -Token $token | Out-Null
    Invoke-Api -Method GET -Path "/api/record/exercise/trend/$userId" -Token $token | Out-Null
    Invoke-Api -Method GET -Path "/api/user/article/list" -Token $token | Out-Null
    Write-Pass "dashboard, glucose metrics, daily aggregation and article list"

    $legacyExport = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri "$BaseUrl/api/record/export" `
        -Headers @{ Authorization = "Bearer $token" } `
        -TimeoutSec 60
    if ($legacyExport.RawContentLength -le 0) {
        throw "Legacy Excel export returned an empty response."
    }
    Write-Pass "synchronous Excel export"

    $pdfReport = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri "$BaseUrl/api/ai/report" `
        -Headers @{ Authorization = "Bearer $token" } `
        -TimeoutSec 60
    $contentDisposition = $pdfReport.Headers["Content-Disposition"]
    if ($pdfReport.RawContentLength -le 0 -or -not $contentDisposition) {
        throw "PDF health report returned an empty response or missing Content-Disposition."
    }
    Write-Pass "PDF health report"

    if (-not $SkipExport) {
        foreach ($type in @("EXCEL", "PDF")) {
            $taskId = [long](Invoke-Api -Method POST -Path "/api/export/tasks" -Token $token -Body @{
                type = $type
            })
            $script:ExportTaskIds.Add($taskId)

            $task = Wait-ExportTask -Token $token -TaskId $taskId
            if ($task.status -ne "SUCCESS") {
                throw "$type export task $taskId failed: $($task.errorMsg)"
            }

            $download = Invoke-WebRequest `
                -UseBasicParsing `
                -Uri "$BaseUrl/api/export/tasks/$taskId/download" `
                -Headers @{ Authorization = "Bearer $token" } `
                -TimeoutSec 60
            if ($download.RawContentLength -le 0) {
                throw "$type export task $taskId downloaded an empty file."
            }
        }
        Invoke-Api -Method GET -Path "/api/export/tasks?page=1&size=10" -Token $token | Out-Null
        Write-Pass "asynchronous Excel/PDF submit, polling, list and download"
    }

    if (-not $SkipAi) {
        $chat = Invoke-Api -Method POST -Path "/api/ai/chat" -Token $token -Body @{
            content = "只回复 OK"
        }
        if (-not $chat.reply) {
            throw "AI chat returned an empty reply."
        }

        $stream = Invoke-WebRequest `
            -UseBasicParsing `
            -Method Post `
            -Uri "$BaseUrl/api/ai/chat/stream" `
            -Headers @{ Authorization = "Bearer $token" } `
            -ContentType "application/json; charset=utf-8" `
            -Body (@{ content = "只回复 OK" } | ConvertTo-Json -Compress) `
            -TimeoutSec 90
        if ($stream.StatusCode -ne 200 -or $stream.RawContentLength -le 0) {
            throw "AI SSE chat returned an invalid response."
        }
        Write-Pass "AI synchronous chat and SSE streaming"
    }

    $adminLogin = New-LoginSession -Username "admin" -Password "123456"
    $adminToken = $adminLogin.Token
    Invoke-Api -Method GET -Path "/api/admin/stats" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/user/list?page=1&size=10" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/article/list?page=1&size=10" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/article/categoryStats" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/exerciseType/list?page=1&size=10" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/audit/list?page=1&size=10" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/risk-user/overview" -Token $adminToken | Out-Null
    Invoke-Api -Method GET -Path "/api/admin/risk-user/list?page=1&size=10" -Token $adminToken | Out-Null
    Write-Pass "admin overview, users, content, exercise, audit and risk users"

    $logout = Invoke-WebRequest `
        -UseBasicParsing `
        -WebSession $userLogin.Session `
        -Method Post `
        -Uri "$BaseUrl/api/user/logout" `
        -TimeoutSec 30
    if ((Get-ResponseJson $logout).code -ne 200) {
        throw "Logout failed."
    }
    Assert-Status -Method Post -Path "/api/user/refresh" -ExpectedStatus 401
    Write-Pass "logout and revoked refresh session"

    Write-Host ""
    Write-Host "PASS: $script:Passed end-to-end groups." -ForegroundColor Green
    if ($script:ExportTaskIds.Count -gt 0) {
        Write-Host "Export task ids created by this run: $($script:ExportTaskIds -join ', ')"
    }
}
finally {
    foreach ($record in $script:CreatedRecords) {
        try {
            Invoke-Api -Method DELETE -Path $record.Path -Token $record.Token | Out-Null
        }
        catch {
            Write-Warning "Failed to clean up $($record.Type) record: $($_.Exception.Message)"
        }
    }
}
