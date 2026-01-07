# run.ps1
param(
  [string]$Config = "config\config_valide.txt"
)

if (-not (Test-Path build)) {
  Write-Host "Le dossier build n'existe pas. Lance d'abord .\build.ps1" -ForegroundColor Yellow
  exit 1
}

java -cp build src.SGBD $Config

