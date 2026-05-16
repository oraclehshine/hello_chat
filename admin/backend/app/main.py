import asyncio
from datetime import datetime

from fastapi import FastAPI, HTTPException, Request, WebSocket
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import get_settings
from app.mock_data import (
    ADMIN_PROFILE,
    ANNOUNCEMENTS,
    AUDIT_LOGS,
    GROUPS,
    METRIC_CARDS,
    REALTIME_EVENTS,
    SENSITIVE_WORDS,
    TODOS,
    USERS,
    trend_points,
)
from app.repositories import (
    announcement_rows,
    audit_log_rows,
    create_announcement,
    create_audit_log,
    create_sensitive_word,
    dashboard_counts,
    dashboard_event_rows,
    dashboard_todo_rows,
    dashboard_trend_rows,
    delete_announcement,
    delete_sensitive_word,
    group_rows,
    refresh_dashboard_todos,
    refresh_metric_snapshot,
    sensitive_word_rows,
    set_announcement_status,
    set_group_status,
    set_sensitive_word_enabled,
    set_user_status,
    user_rows,
)
from app.schemas import AnnouncementPayload, ApiResponse, LoginRequest, SensitiveWordPayload

settings = get_settings()

app = FastAPI(title="Hello Chat Admin API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

API_PREFIX = "/admin/api/v1"


def ok(data: object = None) -> ApiResponse:
    return ApiResponse(data=data)


def client_ip(request: Request) -> str:
    forwarded_for = request.headers.get("x-forwarded-for")
    if forwarded_for:
        return forwarded_for.split(",")[0].strip()
    if request.client:
        return request.client.host
    return "127.0.0.1"


def require_database(row: object | None, message: str = "database table is not ready") -> object:
    if row is None:
        raise HTTPException(status_code=503, detail=message)
    return row


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": "hello-chat-admin-api"}


@app.post(f"{API_PREFIX}/auth/login")
def login(payload: LoginRequest) -> ApiResponse:
    if not payload.username:
        return ApiResponse(code=40001, message="username is required")
    return ok(
        {
            "accessToken": "admin-local-token",
            "admin": ADMIN_PROFILE,
        }
    )


@app.post(f"{API_PREFIX}/auth/logout")
def logout() -> ApiResponse:
    return ok(True)


@app.get(f"{API_PREFIX}/auth/me")
def me() -> ApiResponse:
    return ok(ADMIN_PROFILE)


@app.get(f"{API_PREFIX}/dashboard/overview")
def dashboard_overview() -> ApiResponse:
    counts = dashboard_counts()
    refresh_dashboard_todos()
    refresh_metric_snapshot()
    events = dashboard_event_rows() or REALTIME_EVENTS
    todos = dashboard_todo_rows() or TODOS
    cards = METRIC_CARDS
    if counts:
        private_messages = int(counts.get("private_messages") or 0)
        group_messages = int(counts.get("group_messages") or 0)
        cards = [
            {
                "key": "online",
                "label": "当前在线",
                "value": int(counts.get("online_users") or 0),
                "delta": 0,
                "unit": "人",
            },
            {
                "key": "dau",
                "label": "今日活跃",
                "value": int(counts.get("today_active_users") or 0),
                "delta": int(counts.get("today_new_users") or 0),
                "unit": "人",
            },
            {
                "key": "messages",
                "label": "今日消息",
                "value": private_messages + group_messages,
                "delta": group_messages,
                "unit": "条",
            },
            {
                "key": "moments",
                "label": "朋友圈",
                "value": int(counts.get("moments") or 0),
                "delta": 0,
                "unit": "条",
            },
            {
                "key": "files",
                "label": "文件上传",
                "value": int(counts.get("file_uploads") or 0),
                "delta": 0,
                "unit": "个",
            },
            {
                "key": "hits",
                "label": "敏感词命中",
                "value": 0,
                "delta": 0,
                "unit": "次",
            },
        ]
    return ok(
        {
            "cards": cards,
            "realtimeEvents": events,
            "todos": todos,
        }
    )


@app.get(f"{API_PREFIX}/dashboard/trends")
def dashboard_trends() -> ApiResponse:
    refresh_metric_snapshot()
    return ok(dashboard_trend_rows() or trend_points())


@app.get(f"{API_PREFIX}/dashboard/realtime-events")
def dashboard_events() -> ApiResponse:
    return ok(dashboard_event_rows() or REALTIME_EVENTS)


@app.websocket(f"{API_PREFIX}/dashboard/ws")
async def dashboard_ws(websocket: WebSocket) -> None:
    await websocket.accept()
    counter = 0
    while True:
        counter += 1
        counts = dashboard_counts() or {}
        private_messages = int(counts.get("private_messages") or 0)
        group_messages = int(counts.get("group_messages") or 0)
        await websocket.send_json(
            {
                "type": "metric",
                "title": "实时指标刷新",
                "content": f"当前在线 {int(counts.get('online_users') or 0)} 人，今日消息 {private_messages + group_messages} 条",
                "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            }
        )
        await asyncio.sleep(3)


@app.get(f"{API_PREFIX}/users")
def list_users(keyword: str = "") -> ApiResponse:
    real_rows = user_rows(keyword)
    if real_rows is not None:
        return ok({"list": real_rows, "total": len(real_rows), "source": "database"})
    rows = USERS
    if keyword:
        lower = keyword.lower()
        rows = [
            item
            for item in rows
            if lower in item["email"].lower() or lower in item["nickname"].lower()
        ]
    return ok({"list": rows, "total": len(rows), "source": "mock"})


@app.post(f"{API_PREFIX}/users/{{user_id}}/freeze")
def freeze_user(user_id: int, request: Request) -> ApiResponse:
    row = require_database(set_user_status(user_id, "FROZEN"))
    if "userId" not in row:
        raise HTTPException(status_code=404, detail="user not found")
    create_audit_log("用户管理", "冻结用户", str(user_id), ip=client_ip(request))
    return ok(row)


@app.post(f"{API_PREFIX}/users/{{user_id}}/unfreeze")
def unfreeze_user(user_id: int, request: Request) -> ApiResponse:
    row = require_database(set_user_status(user_id, "ACTIVE"))
    if "userId" not in row:
        raise HTTPException(status_code=404, detail="user not found")
    create_audit_log("用户管理", "解冻用户", str(user_id), ip=client_ip(request))
    return ok(row)


@app.get(f"{API_PREFIX}/groups")
def list_groups(keyword: str = "") -> ApiResponse:
    real_rows = group_rows(keyword)
    if real_rows is not None:
        return ok({"list": real_rows, "total": len(real_rows), "source": "database"})
    rows = GROUPS
    if keyword:
        lower = keyword.lower()
        rows = [item for item in rows if lower in item["groupName"].lower()]
    return ok({"list": rows, "total": len(rows), "source": "mock"})


@app.post(f"{API_PREFIX}/groups/{{group_id}}/disable")
def disable_group(group_id: int, request: Request) -> ApiResponse:
    row = require_database(set_group_status(group_id, 0))
    if "groupId" not in row:
        raise HTTPException(status_code=404, detail="group not found")
    create_audit_log("群聊管理", "禁用群聊", str(group_id), ip=client_ip(request))
    return ok(row)


@app.post(f"{API_PREFIX}/groups/{{group_id}}/enable")
def enable_group(group_id: int, request: Request) -> ApiResponse:
    row = require_database(set_group_status(group_id, 1))
    if "groupId" not in row:
        raise HTTPException(status_code=404, detail="group not found")
    create_audit_log("群聊管理", "启用群聊", str(group_id), ip=client_ip(request))
    return ok(row)


@app.get(f"{API_PREFIX}/sensitive-words")
def list_sensitive_words() -> ApiResponse:
    rows = sensitive_word_rows()
    if rows is not None:
        return ok({"list": rows, "total": len(rows), "source": "database"})
    return ok({"list": SENSITIVE_WORDS, "total": len(SENSITIVE_WORDS), "source": "mock"})


@app.post(f"{API_PREFIX}/sensitive-words")
def add_sensitive_word(payload: SensitiveWordPayload, request: Request) -> ApiResponse:
    row = require_database(create_sensitive_word(payload.model_dump()))
    create_audit_log("敏感词", "新增词条", payload.word, ip=client_ip(request))
    return ok(row)


@app.post(f"{API_PREFIX}/sensitive-words/{{word_id}}/enable")
def enable_sensitive_word(word_id: int, request: Request) -> ApiResponse:
    row = require_database(set_sensitive_word_enabled(word_id, True))
    if "wordId" not in row:
        raise HTTPException(status_code=404, detail="sensitive word not found")
    create_audit_log("敏感词", "启用词条", str(word_id), ip=client_ip(request))
    return ok(row)


@app.post(f"{API_PREFIX}/sensitive-words/{{word_id}}/disable")
def disable_sensitive_word(word_id: int, request: Request) -> ApiResponse:
    row = require_database(set_sensitive_word_enabled(word_id, False))
    if "wordId" not in row:
        raise HTTPException(status_code=404, detail="sensitive word not found")
    create_audit_log("敏感词", "停用词条", str(word_id), ip=client_ip(request))
    return ok(row)


@app.delete(f"{API_PREFIX}/sensitive-words/{{word_id}}")
def remove_sensitive_word(word_id: int, request: Request) -> ApiResponse:
    deleted = require_database(delete_sensitive_word(word_id))
    if not deleted:
        raise HTTPException(status_code=404, detail="sensitive word not found")
    create_audit_log("敏感词", "删除词条", str(word_id), ip=client_ip(request))
    return ok(True)


@app.get(f"{API_PREFIX}/announcements")
def list_announcements() -> ApiResponse:
    rows = announcement_rows()
    if rows is not None:
        return ok({"list": rows, "total": len(rows), "source": "database"})
    return ok({"list": ANNOUNCEMENTS, "total": len(ANNOUNCEMENTS), "source": "mock"})


@app.post(f"{API_PREFIX}/announcements")
def add_announcement(payload: AnnouncementPayload, request: Request) -> ApiResponse:
    row = require_database(create_announcement(payload.model_dump()))
    create_audit_log("公告管理", "创建公告", payload.title, ip=client_ip(request))
    return ok(row)


@app.post(f"{API_PREFIX}/announcements/{{announcement_id}}/publish")
def publish_announcement(announcement_id: int, request: Request) -> ApiResponse:
    row = require_database(set_announcement_status(announcement_id, "published"))
    if "announcementId" not in row:
        raise HTTPException(status_code=404, detail="announcement not found")
    create_audit_log("公告管理", "发布公告", str(announcement_id), ip=client_ip(request))
    return ok(row)


@app.post(f"{API_PREFIX}/announcements/{{announcement_id}}/withdraw")
def withdraw_announcement(announcement_id: int, request: Request) -> ApiResponse:
    row = require_database(set_announcement_status(announcement_id, "withdrawn"))
    if "announcementId" not in row:
        raise HTTPException(status_code=404, detail="announcement not found")
    create_audit_log("公告管理", "撤回公告", str(announcement_id), ip=client_ip(request))
    return ok(row)


@app.delete(f"{API_PREFIX}/announcements/{{announcement_id}}")
def remove_announcement(announcement_id: int, request: Request) -> ApiResponse:
    deleted = require_database(delete_announcement(announcement_id))
    if not deleted:
        raise HTTPException(status_code=404, detail="announcement not found")
    create_audit_log("公告管理", "删除公告", str(announcement_id), ip=client_ip(request))
    return ok(True)


@app.get(f"{API_PREFIX}/audit-logs")
def list_audit_logs() -> ApiResponse:
    rows = audit_log_rows()
    if rows is not None:
        return ok({"list": rows, "total": len(rows), "source": "database"})
    return ok({"list": AUDIT_LOGS, "total": len(AUDIT_LOGS), "source": "mock"})
