$keanDir = $PSScriptRoot
$root = Split-Path -Parent $keanDir
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*#" -or $_ -notmatch "=") { return }
        $k, $v = $_.Split("=", 2)
        Set-Item -Path "Env:$k" -Value $v
    }
}
Set-Location $keanDir
mvn -DskipTests spring-boot:run
