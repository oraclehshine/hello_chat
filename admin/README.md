# Hello Chat Admin

`admin` 是 Hello Chat 的管理端工作区，包含 Vue 管理端前端和 Python 管理端后端。

```text
admin/
├── front/     # Vue 3 + Vite 管理端前端
└── backend/   # Python FastAPI 管理端后端
```

## 本地启动

### 1. 启动管理端后端

```powershell
cd admin/backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```

后端地址：

```text
http://localhost:8090/admin/api/v1
```

### 2. 启动管理端前端

```powershell
cd admin/front
npm install
npm run dev -- --host 0.0.0.0 --port 3100
```

前端地址：

```text
http://localhost:3100
```

## 当前状态

当前版本是可运行骨架，使用模拟数据打通：

- 管理员登录
- 首页实时看板
- 用户管理
- 群聊管理
- 敏感词管理
- 公告管理
- 历史记录
- WebSocket 实时事件流

后续再接入真实 PostgreSQL、Redis、Kafka 和现有 Spring Boot 后端。

## 数据源策略

管理端后端已经支持优先读取 PostgreSQL：

- 用户列表：读取 `users`、`user_presence`、`group_members`、消息表等。
- 群聊列表：读取 `groups`、`users`、`group_members`、`group_messages`。
- 首页看板：读取在线用户、今日活跃、今日消息、朋友圈和文件上传统计。
- 敏感词、公告、历史记录：读取管理端专用表 `sensitive_words`、`admin_announcements`、`admin_operation_logs`。
- 首页实时动态、待办、趋势图：读取管理端专用表 `admin_dashboard_events`、`admin_todo_items`、`admin_metric_snapshots`。

如果数据库不可用，或管理端专属表尚未创建，接口会自动回退到 mock 数据，便于前端继续开发。

如果要连接当前项目 `.env` 中的数据库，可以在 `admin/backend/.env` 中配置：

```env
DB_URL=jdbc:postgresql://localhost:5433/hello_chat
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

管理端专用表可以用下面的脚本初始化：

```powershell
psql -h localhost -p 5433 -U postgres -d hello_chat -f admin/backend/db/admin_schema.sql
```

也可以在后端目录执行：

```powershell
.\.venv\Scripts\python.exe scripts\init_admin_db.py
```

初始化后，敏感词新增/启停/删除、公告创建/发布/撤回/删除、用户冻结/解冻、群聊启用/禁用会优先写入真实数据库，并自动记录到历史记录和首页实时动态；首页待办和趋势图也会使用管理端专用表，不修改主业务表结构。
