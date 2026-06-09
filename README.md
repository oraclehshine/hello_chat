# Hello Chat

Hello Chat 是一个面向 Web 与 Mobile 双端的即时通讯系统，提供用户认证、好友关系、单聊、群聊、朋友圈、文件上传、实时消息推送与容器化部署能力。项目采用统一后端服务支撑多端接入，强调业务闭环、工程可落地性与生产化演进能力。

## 核心能力

- 双端统一接入：Web 端基于 Vue 3，移动端基于 Flutter，共用统一 API 与 WebSocket 通道。
- 实时通信链路：支持会话消息推送、好友关系流转、群组协作与互动通知。
- 社交业务完整：覆盖注册登录、资料维护、好友、群聊、朋友圈、搜索推荐等核心场景。
- 工程化部署：提供 Docker Compose、本地联调脚本、环境变量模板与服务器部署文档。

## 技术栈

- 前端：Vue 3、Vite、TypeScript、Pinia、Axios
- 移动端：Flutter、Dart、Dio
- 后端：Java 17、Spring Boot 3、Spring Data JPA、Flyway、WebSocket
- 基础设施：PostgreSQL、Redis、Kafka、Zookeeper、Nginx、Docker Compose

## 系统结构

```text
hello_chat/
├── backend/                  # Spring Boot 后端服务
├── front/                    # Vue 3 Web 前端
├── mobile/                   # Flutter 移动端
├── admin/                    # 管理端相关资源
├── scripts/                  # 启动与测试脚本
├── docker-compose.yml        # 本地联调编排
├── docker-compose.prod.yml   # 生产部署编排
├── .env.example              # 本地环境变量模板
├── .env.prod.example         # 生产环境变量模板
└── 服务器部署文档.md          # 服务器部署说明
```

## 可用性

项目以稳定可访问为优先目标，设计上尽量减少单点故障与环境耦合：

- 多端统一服务入口，Web 与移动端共享后端能力，降低协议分叉和重复维护成本。
- 前端通过 `/api/v1` 与 `/ws` 代理访问后端，支持本地、局域网与容器环境下的稳定联调。
- 数据层采用 PostgreSQL 持久化核心业务数据，Flyway 管理数据库结构演进，降低版本漂移风险。
- Redis 用于热点状态与会话辅助能力，减轻核心数据库压力，提升高频访问稳定性。
- Docker Compose 提供标准化依赖编排，便于快速恢复开发、测试与部署环境。
- 提供健康检查、环境模板和部署文档，方便问题定位、节点替换与服务重建。

## 高并发设计

项目当前架构已经为高并发演进预留基础能力，重点体现在读写分层、异步解耦与横向扩展友好性：

- API 与 WebSocket 分离业务入口，支持同步请求与实时消息并行处理。
- Redis 可承载在线状态、临时会话数据、热点缓存等高频读场景，减少数据库直接承压。
- Kafka 作为异步消息基础设施，可用于通知分发、事件解耦、削峰填谷与后续多服务扩展。
- 后端采用分层架构，业务逻辑、数据访问与连接管理职责清晰，便于做服务拆分与性能调优。
- 容器化部署天然支持横向扩容，后续可通过 Nginx 或网关对无状态服务实例进行负载均衡。
- 数据库迁移、接口分组和模块边界清晰，为后续分库分表、消息持久化优化、会话分片提供基础。

## 安全性架构设计

项目在认证、配置管理、通信链路和数据边界上采用了基础安全设计，并可持续增强：

- 基于 Token 的统一认证机制，适配 Web 与移动端会话鉴权。
- WebSocket 握手阶段结合鉴权信息校验连接身份，避免匿名长连接直接接入。
- 环境变量承载数据库密码、认证密钥和第三方存储凭据，避免敏感信息硬编码到业务逻辑。
- 文件上传能力与业务接口分离，便于单独做类型校验、大小限制、访问控制与审计扩展。
- 统一响应结构与异常处理机制有助于减少信息泄露，并提升安全事件排查效率。
- 群组、好友、朋友圈等模块具备清晰的资源边界，便于后续补充 RBAC、敏感操作审计、限流、防刷与内容审核策略。

## 主要业务模块

- 认证与账户：注册、登录、验证码、密码找回、资料维护
- 社交关系：好友搜索、好友申请、黑名单、在线状态
- 即时消息：单聊、群聊、消息推送、消息历史
- 社区互动：朋友圈发布、评论、点赞、收藏、举报
- 文件能力：头像、聊天图片与附件上传

## 快速开始

### 1. 启动基础依赖

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

默认访问地址：

```text
http://localhost:3000
```

### 4. 启动移动端

```powershell
cd mobile
.\flutter-env.ps1
cd app
flutter pub get
flutter run
```

## 环境变量

复制 `.env.example` 为 `.env` 后，根据本地环境调整：

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

## 测试与验证

前端构建：

```powershell
cd front
npm run build
```

后端编译：

```powershell
cd backend
mvn -s .mvn-local-settings.xml "-Dmaven.repo.local=.m2repo" -q -DskipTests compile
```

冒烟测试：

```powershell
node scripts/e2e-smoke.mjs
```

模块级回归：

```powershell
node scripts/e2e-modules.mjs
```

## 部署说明

- 本地联调编排：`docker-compose.yml`
- 生产部署编排：`docker-compose.prod.yml`
- 服务器部署文档：[`服务器部署文档.md`](./服务器部署文档.md)

## 演进方向

- 完善多端消息一致性、离线消息与 ACK 机制
- 补充限流、防刷、审计日志与内容安全治理
- 增强监控告警、链路追踪与 CI/CD 自动化能力
- 优化高可用部署能力，支持更细粒度的横向扩展
