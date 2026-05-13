# Hello Chat Flutter Mobile

这个目录用于开发 `Hello Chat` 的 Flutter 移动端。

当前已经完成的准备工作：

- 已迁移设计稿：[app_ui.jsd](/e:/Profile_in_college/linux/code/mobile/app_ui.jsd)
- 已完成 Flutter 与 Android 开发环境配置
- 已补充环境说明文档：[Flutter环境说明.md](/e:/Profile_in_college/linux/code/mobile/Flutter环境说明.md)
- 已提供环境加载脚本：
  - [flutter-env.ps1](/e:/Profile_in_college/linux/code/mobile/flutter-env.ps1)
  - [flutter-env.cmd](/e:/Profile_in_college/linux/code/mobile/flutter-env.cmd)

开发原则：

- 功能与现有 Web 端保持一致
- 视觉风格与现有 Web 端统一
- 布局与交互适配移动端响应式和单手操作
- UI 设计参考 `app_ui.jsd`

当前移动端实现进度：

- 已完成 Flutter 工程初始化与统一主题
- 已接入本地后端 `8083` 端口，支持开发环境和线上 `IP/域名 + :8083` 配置
- 已打通认证流程：
  - 登录
  - 注册
  - 找回密码
- 已打通消息模块：
  - 会话列表
  - 单聊详情
  - 文本消息发送
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
- 已打通朋友圈模块：
  - 时间线
  - 发动态
  - 点赞 / 收藏
  - 评论
  - 互动通知
- 已打通个人资料模块：
  - 查询个人资料
  - 编辑昵称、签名、电话

接下来建议优先推进：

1. WebSocket 实时消息
2. 图片 / 文件上传与发送
3. 朋友圈图片动态与个人主页
4. 群公告、成员管理和审核处理
