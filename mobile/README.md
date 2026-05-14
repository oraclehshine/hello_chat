# Hello Chat Flutter Mobile

这个目录用于开发 `Hello Chat` 的 Flutter 移动端。

当前已完成的准备工作：

- 已迁移设计稿：[app_ui.jsd](/e:/Profile_in_college/linux/code/mobile/app_ui.jsd)
- 已完成 Flutter 与 Android 开发环境配置
- 已补充环境说明文档：[Flutter环境说明.md](/e:/Profile_in_college/linux/code/mobile/Flutter环境说明.md)
- 已提供环境加载脚本：
  - [flutter-env.ps1](/e:/Profile_in_college/linux/code/mobile/flutter-env.ps1)
  - [flutter-env.cmd](/e:/Profile_in_college/linux/code/mobile/flutter-env.cmd)

开发原则：

- 功能与现有 Web 端保持一致
- 视觉风格与现有 Web 端统一
- 布局与交互优先适配移动端响应式和单手操作
- UI 设计参考 `app_ui.jsd`

当前移动端实现进度：

- 已完成 Flutter 工程初始化与统一主题
- 已接入本地后端 `8083` 端口，支持开发环境与线上 `IP/域名 + 端口` 配置
- 已打通认证流程：
  - 登录
  - 注册
  - 找回密码
- 已打通消息模块：
  - 会话列表
  - 单聊详情
  - 文本、图片、文件消息发送
  - 图片全屏预览
  - 文件卡片打开
  - WebSocket 实时消息
- 已打通好友模块：
  - 好友列表
  - 收到的好友申请
  - 已发送申请
  - 搜索用户
  - 发送好友申请
  - 从好友页发起私聊
- 已打通群聊模块：
  - 我的群聊列表
  - 群详情消息流
  - 创建群聊
  - 邀请码入群
  - 搜索群聊
  - 发起入群申请
  - 查看我的入群申请
  - 群聊实时消息
- 已打通朋友圈模块：
  - 时间线
  - 发布文字/图片动态
  - 点赞 / 收藏
  - 评论
  - 互动通知
  - 图片预览
- 已打通个人资料模块：
  - 查询个人资料
  - 编辑昵称、签名、电话

生产编译时可使用：

```bash
flutter build apk --release \
  --dart-define=APP_ENV=prod \
  --dart-define=API_SCHEME=https \
  --dart-define=API_WS_SCHEME=wss \
  --dart-define=API_HOST=your-domain.com \
  --dart-define=API_PORT=8083
```

如果后端通过 `443` 统一反向代理，可以把端口留空：

```bash
flutter build apk --release \
  --dart-define=APP_ENV=prod \
  --dart-define=API_SCHEME=https \
  --dart-define=API_WS_SCHEME=wss \
  --dart-define=API_HOST=api.your-domain.com \
  --dart-define=API_PORT=
```

接下来建议优先推进：

1. 群公告、群成员、群审核处理
2. 朋友圈个人主页与我的动态
3. 更细的错误提示、空状态和分页体验
4. 真机专项适配与发布打包
