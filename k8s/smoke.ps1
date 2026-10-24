[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:30080"
)

$ErrorActionPreference = "Stop"

function Encrypt-Password {
    param(
        [System.Security.Cryptography.RSA]$Rsa,
        [string]$Text
    )

    $bytes = [Text.Encoding]::UTF8.GetBytes($Text)
    $encrypted = $Rsa.Encrypt(
        $bytes,
        [System.Security.Cryptography.RSAEncryptionPadding]::Pkcs1
    )
    return [Convert]::ToBase64String($encrypted)
}

$frontend = Invoke-WebRequest -UseBasicParsing "$BaseUrl/" -TimeoutSec 10
if ($frontend.StatusCode -ne 200) {
    throw "Frontend returned HTTP $($frontend.StatusCode)."
}

$publicKeyResponse = Invoke-RestMethod "$BaseUrl/api/user/public-key"
if ($publicKeyResponse.code -ne 200) {
    throw "Cannot load RSA public key."
}

$rsa = [System.Security.Cryptography.RSA]::Create()
$read = 0
$rsa.ImportSubjectPublicKeyInfo(
    [Convert]::FromBase64String($publicKeyResponse.data),
    [ref]$read
)

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{
    username = "test001"
    password = Encrypt-Password -Rsa $rsa -Text "123456"
} | ConvertTo-Json

$login = Invoke-WebRequest `
    -SkipHttpErrorCheck `
    -WebSession $session `
    -Method Post `
    -Uri "$BaseUrl/api/user/login" `
    -ContentType "application/json" `
    -Body $loginBody

if ($login.StatusCode -ne 200) {
    throw "Login returned HTTP $($login.StatusCode)."
}

$loginData = $login.Content | ConvertFrom-Json
$accessToken = $loginData.data.accessToken
$cookie = $session.Cookies.GetAllCookies() |
    Where-Object Name -eq "refresh_token"

if (-not $accessToken -or -not $cookie) {
    throw "Login did not return access token and refresh cookie."
}

$self = Invoke-WebRequest `
    -SkipHttpErrorCheck `
    -Uri "$BaseUrl/api/user/info/2" `
    -Headers @{ Authorization = "Bearer $accessToken" }

if ($self.StatusCode -ne 200) {
    throw "Authenticated user endpoint returned HTTP $($self.StatusCode)."
}

$refresh = Invoke-WebRequest `
    -SkipHttpErrorCheck `
    -WebSession $session `
    -Method Post `
    -Uri "$BaseUrl/api/user/refresh"

if ($refresh.StatusCode -ne 200) {
    throw "Refresh returned HTTP $($refresh.StatusCode)."
}

$newCookie = $session.Cookies.GetAllCookies() |
    Where-Object Name -eq "refresh_token"

if ($newCookie.Value -eq $cookie.Value) {
    throw "Refresh token was not rotated."
}

$logout = Invoke-WebRequest `
    -SkipHttpErrorCheck `
    -WebSession $session `
    -Method Post `
    -Uri "$BaseUrl/api/user/logout"

if ($logout.StatusCode -ne 200) {
    throw "Logout returned HTTP $($logout.StatusCode)."
}

Write-Host "PASS: frontend, login, access token, refresh rotation and logout."
