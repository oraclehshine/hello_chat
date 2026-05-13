# Mobile 联调前校验清单

## 1. 环境加载
在 PowerShell 执行：

```powershell
cd E:\Profile_in_college\linux\code\mobile
.\flutter-env.ps1
```

预期输出包含：
- `Flutter environment loaded.`
- `FLUTTER_HOME=...`
- `ANDROID_SDK_ROOT=...`
- `JAVA_HOME=...`

## 2. 启动后端
确保后端 `8083` 可用（移动端依赖此端口）。

建议在项目根目录执行（按你已有脚本）：

```powershell
cd E:\Profile_in_college\linux\code
.\start-backend-local.cmd
```

## 3. 启动 Flutter App

```powershell
cd E:\Profile_in_college\linux\code\mobile\app
flutter pub get
flutter run
```

如果你连接了安卓设备，可指定：

```powershell
flutter run -d <deviceId>
```

## 4. 主流程验收
按以下顺序点一遍：

1. 认证页：登录 / 注册 / 找回密码 文案与按钮状态正常。
2. 底部导航：消息 / 好友 / 群聊 / 朋友圈 / 我的 能切换。
3. 消息页：会话列表可展示，能进入单聊详情。
4. 单聊页：发送文本、选择图片/文件按钮可触发。
5. 好友页：搜索用户、发送申请、通过/拒绝申请流程可点通。
6. 群聊页：创建群、邀请码入群、搜索群、申请入群流程可点通。
7. 群详情：消息列表、输入框、发送按钮文案正常。
8. 朋友圈：动态列表、评论弹窗、发布动态弹窗文案正常。
9. 我的页：资料显示、编辑资料、退出登录可用。

## 5. 本轮已完成修复范围
- 文案乱码清理（含坏字符 `�`）
- 关键页面字符串闭合修复
- 主导航与核心流程页中文文案统一

涉及页面：
- `lib/app/shell/app_shell.dart`
- `lib/features/auth/presentation/auth_page.dart`
- `lib/features/chat/presentation/messages_page.dart`
- `lib/features/chat/presentation/chat_detail_page.dart`
- `lib/features/friends/presentation/friends_page.dart`
- `lib/features/groups/presentation/groups_page.dart`
- `lib/features/groups/presentation/group_detail_page.dart`
- `lib/features/moments/presentation/moments_page.dart`
- `lib/features/profile/presentation/profile_page.dart`

## 6. 已知说明
当前终端环境下 `dart format` / `dart analyze` 命令持续超时，未能在这里提供自动化检查结果。
建议你在本机 IDE 内运行：

```powershell
flutter analyze
```

如出现具体报错行号，我可以继续即时修复。
