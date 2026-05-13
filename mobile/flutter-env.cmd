@echo off
set "FLUTTER_HOME=E:\Profile_in_college\linux\tools\flutter"
set "ANDROID_HOME=E:\Profile_in_college\linux\tools\android-sdk"
set "ANDROID_SDK_ROOT=E:\Profile_in_college\linux\tools\android-sdk"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%FLUTTER_HOME%\bin;%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin;%ANDROID_SDK_ROOT%\platform-tools;%JAVA_HOME%\bin;%PATH%"

echo Flutter environment loaded.
echo FLUTTER_HOME=%FLUTTER_HOME%
echo ANDROID_SDK_ROOT=%ANDROID_SDK_ROOT%
echo JAVA_HOME=%JAVA_HOME%
