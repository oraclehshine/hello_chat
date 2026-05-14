# Hello Chat

Hello Chat 是一个覆盖 Web + Mobile 的综合聊天系统，来源于 `hello_chat` 目录中的需求、API、数据库、UI/UX 和环境设计文档，并在 `code` 目录中完成前后端实现。项目覆盖邮箱注册登录、个人资料、好友关系、单聊、群聊、朋友圈、搜索推荐、通知、文件上传、WebSocket 推送、Docker 部署和自动化测试等功能，当前 Web 端（Vue）与移动端（Flutter）均已打通核心业务链路。

## 项目定位

本项目以“真实网络聊天应用”为目标，重点实现聊天系统的完整业务闭环，而不是单一页面演示。系统既包含用户认证、好友关系、会话消息、群组权限、朋友圈互动等应用功能，也包含数据库迁移、环境变量、容器编排、健康检查、开发代理、局域网调试和测试脚本等工程能力；并以统一后端 API 同时支撑 Web 与移动端。

## 技术栈

- 前端：Vue 3、Vite、Vue Router、Pinia、Axios、TypeScript
- 移动端：Flutter、Dart、Dio
- 后端：Java 17、Spring Boot 3.0.3、Spring Data JPA、Flyway、WebSocket
- 数据库：PostgreSQL
- 缓存：Redis
- 消息队列：Kafka + Zookeeper
- 文件存储：阿里云 OSS 配置接口
- 部署：Docker、Docker Compose、Nginx
- 测试：Node.js E2E 脚本、Maven 编译测试、前端构建测试

## 目录结构

```text
code/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/         # Controller、Service、Repository、Entity、DTO、WebSocket
│   ├── src/main/resources/    # application.yml 与 Flyway 数据库迁移
│   └── pom.xml
├── front/                    # Vue 3 前端
│   ├── src/api/               # Axios API 封装与 WebSocket 客户端
│   ├── src/pages/             # 登录、注册、首页、好友、单聊、群聊、朋友圈、个人资料
│   ├── src/styles/            # 全局 Web 工作台样式
│   └── vite.config.ts         # 开发服务与代理配置
├── mobile/                   # Flutter 移动端目录与 UI 参考稿
├── scripts/                  # 启动与自动化测试脚本
├── docker-compose.yml         # PostgreSQL、Redis、Kafka、前后端容器编排
├── docker-compose.prod.yml    # Docker Hub 镜像拉取式生产部署编排
├── .env.example               # 环境变量模板
├── .env.prod.example          # 服务器生产环境变量模板
├── 服务器部署文档.md          # 服务器部署与移动端发布说明
└── README.md
```

## 需求文档摘要

`hello_chat/需求文档.md` 将系统划分为用户管理、单聊、群聊、朋友圈和社交功能模块，并要求在统一后端能力下完成 Web 与移动端的一致性体验。

### 用户管理

- 邮箱注册、邮箱验证码、密码复杂度校验。
- 登录、刷新 Token、退出登录、密码找回。
- 个人资料维护，包括昵称、头像、签名、电话、邮箱等。
- 账户安全操作，如修改密码、修改邮箱。
- 支持 Web 与移动端统一认证流程和会话状态管理（含 Token 过期处理）。

### 单聊

- 好友之间创建一对一会话。
- 支持文本、图片、文件等消息类型。
- 支持消息历史、撤回、删除、清空、搜索和置顶。
- 使用 WebSocket 推送在线消息。
- 支持多端一致的会话列表、消息流与发送交互。

### 群聊

- 创建群组、邀请成员、移除成员、退出群组。
- 群主、管理员、普通成员分级权限。
- 群公告、群通知、入群审批、禁言、群昵称。
- 群消息、@提醒、引用回复和已读状态。
- 支持移动端创建群、邀请码入群、成员选择和群详情消息流。

### 朋友圈

- 发布文字、图片、视频、位置、心情、话题动态。
- 支持公开、好友可见、私密、指定人可见。
- 支持时间线、个人主页、点赞、评论、收藏。
- 支持动态编辑、删除、举报和审核。
- 支持移动端动态发布、评论、点赞收藏和互动通知闭环。

### 社交发现

- 在线状态和最后活跃时间。
- 用户搜索、群组搜索和搜索历史。
- 推荐好友、推荐群组和热门话题。
- 支持移动端好友搜索、好友申请、群搜索与入群申请流程。

## API 设计摘要

API 采用 REST 风格，统一前缀：

```text
/api/v1
```

统一请求头：

```http
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
```

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1715232000000
}
```

主要接口分组：

- `/auth/*`：注册、登录、验证码、刷新 Token、退出登录、密码找回。
- `/users/*`：个人资料、好友申请、好友列表、黑名单、在线状态。
- `/chats/*`：单聊会话、私聊消息、消息撤回和删除。
- `/groups/*`：群组、群成员、群消息、群公告、群通知。
- `/moments/*`：朋友圈动态、点赞、评论、收藏、举报。
- `/social/*`：综合搜索、推荐、热门话题、搜索历史。
- `/files/*`：文件上传与资源访问。
- `/ws/chat`：聊天 WebSocket 推送。

## 数据库设计摘要

项目使用 PostgreSQL 保存核心业务数据，并通过 Flyway 迁移脚本管理结构演进。迁移脚本位于：

```text
backend/src/main/resources/db/migration
```

主要数据模型包括：

- User：用户账号与资料。
- AuthToken / EmailCaptcha：登录令牌与验证码。
- Friendship / FriendRequest / UserBlock：好友关系、申请与黑名单。
- PrivateChat / PrivateMessage：单聊会话与消息。
- ChatGroup / GroupMember / GroupMessage / GroupNotification：群组、成员、群消息和通知。
- Moment / MomentComment / MomentLike / MomentCollect / MomentReport：朋友圈动态、评论、点赞、收藏和举报。
- SearchHistory / UserPresence：搜索历史与在线状态。
- FileAsset：上传文件元信息。

## 前端实现摘要

前端位于 `front`，采用 Vue 3 单页应用结构。

- `src/api/http.ts`：统一 Axios 实例，自动附加 Token。
- `src/api/chatSocket.ts`：WebSocket 客户端连接与消息事件解析。
- `src/router/index.ts`：页面路由和登录态跳转。
- `src/pages/LoginPage.vue`：登录页。
- `src/pages/RegisterPage.vue`：注册页与验证码流程。
- `src/pages/HomePage.vue`：消息工作台概览。
- `src/pages/FriendPage.vue`：好友、搜索、请求和黑名单。
- `src/pages/ChatPage.vue`：单聊会话。
- `src/pages/GroupPage.vue`：群聊管理与群消息。
- `src/pages/MomentPage.vue`：朋友圈动态流。
- `src/pages/ProfilePage.vue`：Web 适配的个人资料页。

开发环境中，Vite 绑定 `0.0.0.0:3000`，并代理：

```text
/api/v1 -> http://localhost:8083
/ws     -> ws://localhost:8083
```

这样局域网设备访问 `http://192.168.x.x:3000` 时，请求会由 Vite 转发到本机后端，避免其他设备把 `localhost` 解析成自身导致 Network error。

## 移动端实现摘要

移动端位于 `mobile/app`，采用 Flutter 实现，当前已与后端 `:8083` 接口打通，并具备核心业务闭环。

- 认证：登录、注册、找回密码
- 消息：会话列表、单聊详情、文本消息发送
- 好友：好友列表、搜索用户、发送申请、收发申请处理
- 群聊：我的群聊、群详情、创建群聊、邀请码入群、搜索群、入群申请
- 朋友圈：动态列表、发布、点赞、收藏、评论、互动通知
- 我的：资料查询、资料编辑、退出登录
- 全局会话：Token 过期自动退出并回到登录页

主要目录：

```text
mobile/
├── flutter-env.ps1            # PowerShell 环境加载脚本
├── flutter-env.cmd            # CMD 环境加载脚本
└── app/                       # Flutter 工程
    ├── lib/app/               # App 入口、主题、导航
    ├── lib/core/              # 网络、模型、存储、服务
    ├── lib/features/          # 认证/消息/好友/群聊/朋友圈/我的
    └── MOBILE_QA_CHECKLIST.md # 联调与验收清单
```

移动端本地启动：

```powershell
cd mobile
.\flutter-env.ps1
cd app
flutter pub get
flutter run
```

联调前建议先启动后端（`8083`），并按以下清单回归主流程：

```text
mobile/app/MOBILE_QA_CHECKLIST.md
```

## 后端实现摘要

后端位于 `backend`，采用 Spring Boot 分层架构。

- `controller`：提供 REST API。
- `service` / `service.impl`：实现认证、用户、好友、单聊、群聊、朋友圈、文件等业务逻辑。
- `repository`：使用 Spring Data JPA 访问 PostgreSQL。
- `entity`：定义业务实体。
- `dto`：定义请求与响应对象。
- `common`：统一响应、异常处理、Token、密码编码、当前用户解析。
- `websocket`：WebSocket 连接管理和 Token 握手鉴权。
- `config`：CORS、OSS、WebSocket 等配置。

核心配置使用环境变量注入：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5433/hello_chat}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:wang5874579%}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
server:
  port: ${SERVER_PORT:8083}
```

## 环境变量

复制 `.env.example` 为 `.env`，根据本机环境修改：

```env
DB_URL=jdbc:postgresql://localhost:5433/hello_chat
DB_USERNAME=postgres
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SERVER_PORT=8083
HELLO_CHAT_AUTH_SECRET=change-me
ALIYUN_OSS_ACCESS_KEY_ID=
ALIYUN_OSS_ACCESS_KEY_SECRET=
```

注意：真实密钥和密码不要提交到公开仓库。

## 本地开发启动

### 1. 启动依赖服务

```powershell
docker compose up -d postgres redis zookeeper kafka
```

### 2. 启动后端

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```

或手动执行：

```powershell
cd backend
mvn -s .mvn-local-settings.xml "-Dmaven.repo.local=.m2repo" spring-boot:run
```

### 3. 启动前端

```powershell
cd front
npm install
npm run dev -- --host 0.0.0.0 --port 3000
```

访问地址：

```text
http://localhost:3000
```

局域网访问地址以 Vite 输出为准，例如：

```text
http://192.168.x.x:3000
```

## Docker Compose 部署

完整容器化启动：

```powershell
docker compose up --build
```

服务端口：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8083`
- PostgreSQL：`localhost:5433`
- Redis：`localhost:6379`
- Kafka：`localhost:9092`
- 健康检查：`http://localhost:8083/actuator/health`

## 开发过程补充

本项目不是一次性静态实现，而是基于 `hello_chat` 目录中的原始需求文档持续迭代完成。开发过程大致分为以下几个阶段：

### 1. 基础功能落地

- 按需求文档拆分用户管理、好友、单聊、群聊、朋友圈、社交搜索、通知与文件模块。
- 后端先完成数据库迁移、实体关系、REST API 和 WebSocket 通道。
- 前端先完成登录、注册、概览页、好友页、单聊页、群聊页、朋友圈页和个人资料页的基本业务闭环。

### 2. 开发环境与联调打通

- 将前端开发服务固定为 `0.0.0.0:3000`，便于局域网设备访问。
- 使用 Vite 代理 `/api/v1` 和 `/ws`，解决“其他设备访问出现 Network error”问题。
- 将数据库、Kafka、Redis 等依赖与项目启动脚本解耦，支持“依赖在 Docker 中运行，前后端本地直接启动”的开发方式。
- 增加 `start-backend-local.cmd`、`start-front-local.cmd`，简化本地联调。

### 3. 文件上传与 OSS 问题处理

- 将前端文件上传改为浏览器自动处理 `multipart/form-data` 边界，避免手动设置请求头导致上传失败。
- 后端补充 OSS 相关错误日志，输出 `bucket`、`endpoint`、上传场景和异常类型，便于定位上传问题。
- 根据实际报错排查出上传限制问题，将 Spring multipart 大小限制提升，并为超限错误增加明确返回。
- 资源访问侧补充统一的资源 URL 解析逻辑，用于头像、聊天图片、群图和朋友圈媒体展示。

### 4. 自动化测试与验证

- 编写 `scripts/e2e-smoke.mjs`，覆盖登录注册、好友、私聊、群聊、朋友圈等核心链路。
- 编写 `scripts/e2e-modules.mjs`，对认证、用户好友、社交、单聊、群聊、朋友圈、文件模块做更细粒度验证。
- 在每轮主要前端改动后执行 `npm run build`，在主要后端改动后执行 Maven 编译，保证改动可构建。

### 5. Web UI 重构与体验优化

在联调完成后，前端进入了较大规模的 Web 适配与视觉重构阶段，重点包括：

- 将登录/注册入口与主功能区彻底拆开，避免首页既是登录又是应用工作台。
- 统一侧边导航、主内容区、卡片、抽屉和弹层的玻璃态风格。
- 引入统一头像组件 `AvatarFrame.vue`，让个人资料、好友、单聊、群聊、朋友圈使用一致的头像展示方式。
- 将好友、群聊、朋友圈中的复杂操作收进更适合 Web 的抽屉、工具栏和右上角入口中，避免移动端式平铺。
- 将单聊与群聊发送器统一为“输入框 + 工具条 + 发送按钮”的主流 Web IM 布局。
- 将消息操作改为右键菜单，减少常驻按钮噪声。
- 将朋友圈评论区改成“头像 + 评论气泡 + 二级回复 + 回复输入器”的统一结构。

### 6. 登录页品牌与动效迭代

- 登录页左侧海报区重做为品牌展示区，统一使用 `dog.svg` 作为项目 logo。
- 去掉冗余说明文字和重复标题，使登录页更简洁。
- 增加小猫动效、消息气泡、连线和下落消息点，让海报区更像一个“沟通流动”的场景，而不是静态插图。
- 多轮调整动画区与标题区的间距、层级和位置，避免遮挡文字或压住小猫。

### 7. 文档与课程材料整理

- 将 `hello_chat` 目录中的需求、环境、数据库、API、UI/UX 文档汇总到实现说明中。
- 在 `code/README.md` 中补齐启动、联调、测试、问题排查、Linux 课程关联和后续展望。
- 额外整理课程报告、测试结果和截图材料，便于作业提交与展示。

### 当前实现状态

截至当前版本，项目已完成：

- 认证、注册、找回密码、邮箱验证码
- 个人资料维护与头像上传
- 好友搜索、推荐、申请、黑名单
- 单聊消息、图片/文件、撤回、删除、置顶、搜索
- 群聊消息、公告、成员管理、审核、禁言、群昵称、群资料
- 朋友圈动态、评论、点赞、收藏、举报、通知
- WebSocket 实时推送
- Docker 依赖与本地开发联调
- 冒烟测试与模块级测试脚本

当前仍可继续优化的方向包括：

- 更完整的移动端适配
- 更精细的动画和页面进入反馈
- 更完整的生产级日志、监控和 CI/CD
- 更强的内容审核、限流和安全治理

## 测试与验证

### 前端构建

```powershell
cd front
npm run build
```

### 后端编译

```powershell
cd backend
mvn -s .mvn-local-settings.xml "-Dmaven.repo.local=.m2repo" -q -DskipTests compile
```

### 冒烟测试

```powershell
node scripts/e2e-smoke.mjs
```

该脚本会创建临时用户并覆盖认证、资料、好友、单聊、群聊、朋友圈、通知、搜索推荐和密码流程。

### 模块级回归测试

```powershell
node scripts/e2e-modules.mjs
```

该脚本按认证、用户好友、社交、单聊、群聊、朋友圈、文件上传等模块进行更细粒度验证，并包含部分预期失败场景。

## 常见问题

### 其他设备访问出现 Network error

原因通常是前端把接口写成 `http://localhost:8083`。其他设备访问时，`localhost` 指向设备自身，不是运行后端的电脑。

当前项目已通过 Vite 代理解决：前端默认请求 `/api/v1`，再由开发服务器转发到本机后端。

### 后端连接 PostgreSQL 失败

如果出现：

```text
Connection to localhost:5433 refused
```

请先确认容器和端口：

```powershell
docker compose ps
netstat -ano | Select-String ':5433|:6379|:9092'
```

### Maven 权限问题

如果 Maven 尝试写入系统仓库失败，请使用项目内仓库：

```powershell
mvn -s .mvn-local-settings.xml "-Dmaven.repo.local=.m2repo" compile
```

## 与 Linux 操作系统课程的关系

本项目实践了 Linux 操作系统课程中的多项核心能力：

- 进程管理：前端、后端、数据库、缓存、消息队列均以独立服务进程运行。
- 网络通信：涉及 HTTP、WebSocket、端口监听、端口映射、局域网访问和反向代理。
- 文件系统：涉及项目目录组织、配置文件、日志文件、上传文件和数据库迁移脚本。
- 权限与环境：使用环境变量管理密码、Token 密钥和 OSS 凭据，处理 Maven、Docker 等权限问题。
- 服务部署：使用 Docker Compose 编排多服务，理解容器网络与服务依赖。
- 故障排查：通过日志、健康检查、端口检测、接口测试定位问题。

## 后续展望

- 持续优化 Flutter 移动端体验（实时消息、上传交互、页面动效与性能）。
- 增加更完整的消息 ACK、离线消息和多端同步机制。
- 增加接口限流、审计日志、敏感词过滤和内容审核能力。
- 优化移动端适配、图片预览、消息虚拟列表和暗色模式。
- 引入 CI/CD，在提交代码后自动运行构建和测试。
- 增加 Prometheus、Grafana、ELK/Loki 等监控日志体系。
- 将 Docker Compose 部署升级为 Kubernetes 编排，进一步贴近生产环境。
