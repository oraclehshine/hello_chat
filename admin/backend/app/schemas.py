from pydantic import BaseModel, Field


class ApiResponse(BaseModel):
    code: int = 0
    message: str = "success"
    data: object | None = None


class LoginRequest(BaseModel):
    username: str
    password: str


class SensitiveWordPayload(BaseModel):
    word: str
    scene: str = "chat"
    level: str = "medium"
    action: str = "warn"
    enabled: bool = True


class AnnouncementPayload(BaseModel):
    title: str
    content: str
    scope: str = "all"
    status: str = "draft"


class AdminProfile(BaseModel):
    admin_id: int = Field(alias="adminId")
    username: str
    display_name: str = Field(alias="displayName")
    role: str
    permissions: list[str]


class LoginResponse(BaseModel):
    access_token: str = Field(alias="accessToken")
    admin: AdminProfile


class MetricCard(BaseModel):
    key: str
    label: str
    value: int
    delta: int
    unit: str = ""


class DashboardOverview(BaseModel):
    cards: list[MetricCard]
    realtime_events: list[dict] = Field(alias="realtimeEvents")
    todos: list[dict]


class TrendPoint(BaseModel):
    label: str
    online_users: int = Field(alias="onlineUsers")
    messages: int
    sensitive_hits: int = Field(alias="sensitiveHits")


class UserRow(BaseModel):
    user_id: int = Field(alias="userId")
    email: str
    nickname: str
    status: str
    online: bool
    message_count: int = Field(alias="messageCount")
    group_count: int = Field(alias="groupCount")
    risk_level: str = Field(alias="riskLevel")
    last_active_at: str = Field(alias="lastActiveAt")


class GroupRow(BaseModel):
    group_id: int = Field(alias="groupId")
    group_name: str = Field(alias="groupName")
    owner_name: str = Field(alias="ownerName")
    status: str
    member_count: int = Field(alias="memberCount")
    today_messages: int = Field(alias="todayMessages")
    sensitive_hits: int = Field(alias="sensitiveHits")
    report_count: int = Field(alias="reportCount")


class SensitiveWordRow(BaseModel):
    word_id: int = Field(alias="wordId")
    word: str
    scene: str
    level: str
    action: str
    enabled: bool
    hit_count: int = Field(alias="hitCount")


class AnnouncementRow(BaseModel):
    announcement_id: int = Field(alias="announcementId")
    title: str
    scope: str
    status: str
    read_count: int = Field(alias="readCount")
    created_at: str = Field(alias="createdAt")


class AuditLogRow(BaseModel):
    log_id: int = Field(alias="logId")
    admin_name: str = Field(alias="adminName")
    module: str
    action: str
    target: str
    ip: str
    created_at: str = Field(alias="createdAt")
