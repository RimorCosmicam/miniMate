$ErrorActionPreference = "Stop"
$Project = Split-Path -Parent $MyInvocation.MyCommand.Path
dotnet publish "$Project\MiniMateAudio.csproj" `
  -c Release `
  -r win-x64 `
  --self-contained true `
  -p:PublishSingleFile=true `
  -p:IncludeNativeLibrariesForSelfExtract=true `
  -o "$Project\dist\win-x64"
Write-Host "$Project\dist\win-x64\MiniMateAudio.exe"
