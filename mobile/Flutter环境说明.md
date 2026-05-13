# Flutter 环境说明

## 当前状态

移动端 Flutter 开发环境已在本机完成配置，并通过 `flutter doctor -v` 验证。

验证结果摘要：

- Flutter：正常
- Dart：正常
- Android toolchain：正常
- Android licenses：已接受
- Chrome：正常
- Windows Desktop：正常

`flutter doctor -v` 最终结果：

```text
No issues found!
```

## 安装位置

按照“除 Java 外尽量安装到 E 盘”的要求，当前工具链位置如下：

- Flutter SDK：`E:\Profile_in_college\linux\tools\flutter`
- Android SDK：`E:\Profile_in_college\linux\tools\android-sdk`
- Android Command-line Tools：`E:\Profile_in_college\linux\tools\android-sdk\cmdline-tools\latest`
- Java JDK：`C:\Program Files\Java\jdk-17`

说明：

- Java 保留在 `C:`，这是本机现有可用 JDK。
- Flutter 与 Android SDK 已统一放在 `E:` 盘项目工作区下。

## 已配置环境变量

已写入当前用户环境变量：

- `FLUTTER_HOME=E:\Profile_in_college\linux\tools\flutter`
- `ANDROID_HOME=E:\Profile_in_college\linux\tools\android-sdk`
- `ANDROID_SDK_ROOT=E:\Profile_in_college\linux\tools\android-sdk`
- `JAVA_HOME=C:\Program Files\Java\jdk-17`

并已加入 `PATH`：

- `E:\Profile_in_college\linux\tools\flutter\bin`
- `E:\Profile_in_college\linux\tools\android-sdk\cmdline-tools\latest\bin`
- `E:\Profile_in_college\linux\tools\android-sdk\platform-tools`
- `C:\Program Files\Java\jdk-17\bin`

## 已安装 Android 组件

当前已安装：

- `platform-tools`
- `platforms;android-35`
- `platforms;android-36`
- `build-tools;35.0.0`
- `build-tools;28.0.3`
- `cmdline-tools;latest`

## 本地使用方式

### 方式一：新开终端后直接使用

如果系统已经重新读取用户环境变量，可以直接运行：

```powershell
flutter --version
flutter doctor -v
```

### 方式二：先加载项目内环境脚本

如果你当前开的终端还没刷新环境变量，先执行：

```powershell
. .\code\mobile\flutter-env.ps1
```

然后再运行：

```powershell
flutter --version
flutter doctor -v
adb --version
```

如果你用 `cmd`，可以执行：

```cmd
code\mobile\flutter-env.cmd
```

## 推荐检查命令

```powershell
flutter --version
flutter doctor -v
flutter config --list
adb --version
```

## 后续开发建议

### 1. 初始化 Flutter 项目

建议在 `code/mobile` 下执行：

```powershell
cd code\mobile
flutter create app
```

### 2. 推荐目录

建议后续使用：

```text
code/mobile/app
```

作为真正的 Flutter 工程目录，保留当前 `code/mobile` 作为移动端总入口，便于同时放：

- 设计稿
- 环境说明
- 开发记录
- Flutter 工程

### 3. 真机调试

若后续要调 Android 真机：

1. 手机开启开发者模式与 USB 调试
2. 连接电脑
3. 运行 `adb devices`
4. 运行 `flutter devices`

### 4. 模拟器说明

当前环境已具备 Flutter + Android SDK 命令行开发能力，但尚未额外安装 Android Studio 图形界面与 AVD 模拟器。

如果后续你需要：

- 图形化管理 SDK
- 创建 Android 模拟器
- 使用 Android Studio 调试

再补装 Android Studio 即可，不影响当前 Flutter 项目开发准备。
