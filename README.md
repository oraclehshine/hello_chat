# Hello Chat 代码目录说明

## 目录结构

```text
code/
├── backend/   # Java 17 + Spring Boot 3.0.3 后端
└── front/     # Vue 3 + Vite 前端
```

## 技术栈

- 前端：Vue 3 + Vite + Vue Router + Pinia
- 后端：Java 17 + Spring Boot 3.0.3
- 数据库：PostgreSQL
- 缓存：Redis
- 消息队列：Kafka

## 本地环境

- PostgreSQL：localhost:5433
- 用户名：postgres
- 密码：wang5874579%
- Redis：localhost:6379（无密码）
- Kafka：localhost:9092

## 启动顺序

1. 启动 PostgreSQL、Redis、Kafka
2. 启动 backend
3. 启动 front

## 本地启动

### 后端

```bash
cd backend
mvn -s .mvn-local-settings.xml '-Dmaven.repo.local=.m2repo' spring-boot:run
```

### 前端

```bash
cd front
npm install
npm run dev
```

## 容器部署

1. 复制 `.env.example` 为 `.env`
2. 修改 `.env` 中的数据库密码、JWT 密钥和 OSS 凭据
3. 启动服务

```bash
docker compose up --build
```

服务入口：
- 前端：http://localhost:3000
- 后端：http://localhost:8083
- 健康检查：http://localhost:8083/actuator/health

## 当前开发阶段

- 第 6 阶段：优化和部署（Phase 6）
- 已完成：
  - Phase 1-5 核心业务功能
  - 朋友圈动态发布、编辑、删除、分页浏览
  - 点赞/取消点赞、评论/删除评论、收藏/取消收藏
  - 个人动态流与收藏流
  - 动态举报、审核处理、通知中心（未读计数与已读管理）
  - 在线状态、综合搜索、搜索历史和推荐系统
  - 环境变量化配置、Actuator 健康检查、Docker/Compose 部署骨架
  - 认证基础单元测试
