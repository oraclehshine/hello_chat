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

## 当前开发阶段

- 第 4 阶段：朋友圈功能（Phase 4）
- 已完成：
  - 朋友圈动态发布、编辑、删除、分页浏览
  - 点赞/取消点赞、评论/删除评论、收藏/取消收藏
  - 个人动态流与收藏流
  - 动态举报、审核处理、通知中心（未读计数与已读管理）
