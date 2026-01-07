# build.ps1
if (Test-Path build) { Remove-Item -Recurse -Force build }
New-Item -ItemType Directory -Path build | Out-Null

javac -encoding UTF-8 -d build src\*.java test\*.java

if ($LASTEXITCODE -ne 0) {
  Write-Host "Compilation échouée." -ForegroundColor Red
  exit 1
}

Write-Host "Compilation OK." -ForegroundColor Green
