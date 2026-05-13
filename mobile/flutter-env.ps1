$env:FLUTTER_HOME = "E:\Profile_in_college\linux\tools\flutter"
$env:ANDROID_HOME = "E:\Profile_in_college\linux\tools\android-sdk"
$env:ANDROID_SDK_ROOT = "E:\Profile_in_college\linux\tools\android-sdk"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

$paths = @(
  "$env:FLUTTER_HOME\bin",
  "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin",
  "$env:ANDROID_SDK_ROOT\platform-tools",
  "$env:JAVA_HOME\bin"
)

$env:Path = ($paths -join ";") + ";" + $env:Path

Write-Host "Flutter environment loaded." -ForegroundColor Green
Write-Host "FLUTTER_HOME=$env:FLUTTER_HOME"
Write-Host "ANDROID_SDK_ROOT=$env:ANDROID_SDK_ROOT"
Write-Host "JAVA_HOME=$env:JAVA_HOME"
