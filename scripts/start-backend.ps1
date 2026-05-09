param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $repoRoot "backend"
$envPath = Join-Path $repoRoot $EnvFile

if (Test-Path $envPath) {
    Get-Content $envPath | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) {
            return
        }

        $separatorIndex = $line.IndexOf("=")
        if ($separatorIndex -le 0) {
            return
        }

        $name = $line.Substring(0, $separatorIndex).Trim()
        $value = $line.Substring($separatorIndex + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
    Write-Host "Loaded environment file: $envPath"
} else {
    Write-Host "Environment file not found: $envPath"
    Write-Host "Continuing with existing process environment and application defaults."
}

Push-Location $backendDir
try {
    mvn -s .mvn-local-settings.xml "-Dmaven.repo.local=.m2repo" spring-boot:run
} finally {
    Pop-Location
}
