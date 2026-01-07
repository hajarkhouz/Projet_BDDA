# all.ps1
param(
  [string]$Config = "config\config_valide.txt"
)

if (Test-Path build) { Remove-Item -Recurse -Force build }
New-Item -ItemType Directory -Path build | Out-Null

javac -encoding UTF-8 -d build src\*.java test\*.java
if ($LASTEXITCODE -ne 0) { exit 1 }

java -cp build src.SGBD $Config
