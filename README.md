# Hello Chat

Hello Chat 是一个面向 Web 与 Mobile 双端的即时通信系统，覆盖用户认证、好友关系、单聊、群聊、朋友圈、文件上传、实时消息推送与容器化部署能力。项目当前已经完成一轮以高并发与可扩展性为目标的后端改造，核心方向是把热路径从“单体同步直推”演进为“缓存承载 + 事件解耦 + 多节点路由”。

## 核心能力

- 统一接入：Web 端基于 Vue 3，移动端基于 Flutter，共享统一 REST API 与 WebSocket 通道
- 即时通信：支持单聊、群聊、消息历史、消息撤回、群公告、好友在线状态
- 社交扩展：支持朋友圈、评论、点赞、收藏、搜索与推荐类业务
- 工程化部署：提供 Docker Compose、本地启动脚本、环境变量模板与部署文档

## 技术栈

- 前端：Vue 3、Vite、TypeScript、Pinia、Axios
- 移动端：Flutter、Dart、Dio
- 后端：Java 17、Spring Boot 3、Spring Data JPA、Flyway、WebSocket
- 基础设施：PostgreSQL、Redis、Kafka、Zookeeper、Nginx、Docker Compose
- 高并发治理：Spring Cloud Alibaba Nacos、Sentinel

## 系统结构

```text
hello_chat/
├── backend/                  # Spring Boot 后端服务
├── front/                    # Vue 3 Web 前端
├── mobile/                   # Flutter 移动端
├── admin/                    # 管理端相关资源
├── doc/                      # 架构设计、改造文档、更新记录
├── scripts/                  # 启动与测试脚本
├── docker-compose.yml        # 本地联调编排
├── docker-compose.prod.yml   # 生产部署编排
├── .env.example              # 本地环境变量模板
└── .env.prod.example         # 生产环境变量模板
```

## 可用性

- 后端核心状态逐步外置到 Redis，避免单节点内存状态成为扩容瓶颈
- WebSocket 路由已改为 Redis 承载的多节点会话注册模型，支持同一用户多端在线
- 聊天与推送链路通过 Kafka 解耦，消息写库与消息分发不再强耦合在同一请求线程
- Sentinel 已接入登录、验证码、单聊发消息、群聊发消息、文件上传等热点入口
- 异步线程池已承接运营统计类写入，避免后台记录阻塞主业务响应

## 高并发设计

项目当前已经完成的高并发改造重点集中在后端热路径：

- Spring Cloud Alibaba 基础接入：引入 Nacos、Sentinel，为配置中心、服务发现和流量治理预留能力
- 单聊链路 Kafka 化：`ChatPushServiceImpl -> ChatPushKafkaListener -> LocalChatPushDispatcher`
- 群聊链路 Kafka 化：`GroupPushServiceImpl -> GroupPushKafkaListener -> LocalGroupPushDispatcher`
- WebSocket 分发 Kafka 化：`KafkaWebSocketDispatchPublisher -> WebSocketDispatchKafkaListener -> WebSocketDispatchDispatcher`
- Redis 热数据承载：
  - 单聊在线状态、未读数、会话摘要
  - 群聊未读数、`@提及` 未读、公告未读、群摘要
- Kafka 幂等消费门闩：通过 Redis 对 `chat-push`、`group-push`、`websocket-dispatch` 事件做去重，降低重复投递带来的重复推送风险

详细设计、代码定位与改造说明见：

- [高并发设计与改造总览](./doc/高并发设计与改造总览.md)
- [高并发改造建议文档](./doc/高并发改造建议文档.md)
- [高并发改造续做进展](./doc/update/2026-06-09-高并发改造续做进展.md)

## 安全性架构设计

- 统一 Token 鉴权，WebSocket 握手阶段校验身份
- Sentinel 对热点接口限流，降低暴力请求与异常洪峰风险
- Redis 与 Kafka 事件链路分层隔离，减少同步串行路径暴露面
- 文件上传与业务接口分离，便于独立施加大小限制、类型校验与访问控制
- DTO、控制器、服务、仓储边界清晰，便于后续补充审计日志、RBAC、幂等控制和死信治理

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
- 服务器部署文档：`服务器部署文档.md`

## 演进方向

- 完善 Kafka 重试、死信、消息幂等键与消费监控
- 继续拆分 `ws-gateway / chat / group` 服务边界
- 加强 Redis 热数据监控、链路追踪、限流策略与安全审计
- 为后续分库分表、读写分离和多实例网关扩容做好准备
