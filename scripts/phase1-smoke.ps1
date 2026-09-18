# Phase 1 接口冒烟。依赖已启动的后端 http://127.0.0.1:8080

$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8080"
$username = "student_" + (Get-Random -Maximum 999999)

function Invoke-Json {
    param([string]$Method, [string]$Url, [string]$Body, [string]$Token)
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    if ($Body) {
        return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body $Body
    }
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers
}

$registerBody = @{
    username = $username
    password = "Passw0rd!"
    nickname = "测试用户"
    schoolId = 1
    campusId = 1
} | ConvertTo-Json

$register = Invoke-Json -Method POST -Url "$base/api/auth/register" -Body $registerBody
if ($register.code -ne 0) { throw "register failed: $($register | ConvertTo-Json -Compress)" }

$login = Invoke-Json -Method POST -Url "$base/api/auth/login" -Body (@{ username = $username; password = "Passw0rd!" } | ConvertTo-Json)
if (-not $login.data.token) { throw "login failed" }
$token = $login.data.token

$me = Invoke-Json -Method GET -Url "$base/api/auth/me" -Token $token
if ($me.data.username -ne $username) { throw "me mismatch" }

Invoke-Json -Method POST -Url "$base/api/auth/logout" -Token $token | Out-Null

try {
    Invoke-WebRequest -Method GET -Uri "$base/api/auth/me" -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing | Out-Null
    throw "logout token still valid"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw }
}

$adminLogin = Invoke-Json -Method POST -Url "$base/api/auth/login" -Body (@{ username = "admin"; password = "ChangeMe_Admin_123" } | ConvertTo-Json)
if ($adminLogin.data.user.role -ne "ADMIN") { throw "admin login failed" }

Write-Output "Phase 1 smoke OK: register/login/me/logout/admin"
