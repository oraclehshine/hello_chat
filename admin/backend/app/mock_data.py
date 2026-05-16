from datetime import datetime, timedelta


def now_text(minutes_ago: int = 0) -> str:
    return (datetime.now() - timedelta(minutes=minutes_ago)).strftime(
        "%Y-%m-%d %H:%M:%S"
    )


ADMIN_PROFILE = {
    "adminId": 1,
    "username": "admin",
    "displayName": "平台管理员",
    "role": "super_admin",
    "permissions": [
        "dashboard:view",
        "users:manage",
        "groups:manage",
        "sensitive_words:manage",
        "announcements:manage",
        "audit_logs:view",
    ],
}

METRIC_CARDS = [
    {"key": "online", "label": "当前在线", "value": 238, "delta": 12, "unit": "人"},
    {"key": "dau", "label": "今日活跃", "value": 1860, "delta": 87, "unit": "人"},
    {"key": "messages", "label": "今日消息", "value": 42890, "delta": 3210, "unit": "条"},
    {"key": "groups", "label": "活跃群聊", "value": 126, "delta": 8, "unit": "个"},
    {"key": "reports", "label": "待处理举报", "value": 14, "delta": -3, "unit": "条"},
    {"key": "hits", "label": "敏感词命中", "value": 37, "delta": 5, "unit": "次"},
]

REALTIME_EVENTS = [
    {"type": "risk", "title": "群聊命中敏感词", "content": "学习交流群出现 3 次风险词", "time": now_text(1)},
    {"type": "user", "title": "用户活跃增长", "content": "最近 10 分钟新增在线用户 28 人", "time": now_text(3)},
    {"type": "report", "title": "新的朋友圈举报", "content": "动态 #1024 收到内容举报", "time": now_text(5)},
]

TODOS = [
    {"label": "待处理举报", "value": 14, "level": "high"},
    {"label": "待审核入群申请", "value": 21, "level": "medium"},
    {"label": "敏感词误报待确认", "value": 6, "level": "medium"},
]

USERS = [
    {
        "userId": 1,
        "email": "alice@example.com",
        "nickname": "Alice",
        "status": "normal",
        "online": True,
        "messageCount": 982,
        "groupCount": 8,
        "riskLevel": "low",
        "lastActiveAt": now_text(2),
    },
    {
        "userId": 2,
        "email": "bob@example.com",
        "nickname": "Bob",
        "status": "normal",
        "online": False,
        "messageCount": 341,
        "groupCount": 3,
        "riskLevel": "medium",
        "lastActiveAt": now_text(48),
    },
    {
        "userId": 3,
        "email": "risk@example.com",
        "nickname": "RiskUser",
        "status": "frozen",
        "online": False,
        "messageCount": 112,
        "groupCount": 1,
        "riskLevel": "high",
        "lastActiveAt": now_text(180),
    },
]

GROUPS = [
    {
        "groupId": 101,
        "groupName": "课程项目交流群",
        "ownerName": "Alice",
        "status": "active",
        "memberCount": 86,
        "todayMessages": 1230,
        "sensitiveHits": 2,
        "reportCount": 0,
    },
    {
        "groupId": 102,
        "groupName": "游戏开黑群",
        "ownerName": "Bob",
        "status": "active",
        "memberCount": 43,
        "todayMessages": 584,
        "sensitiveHits": 5,
        "reportCount": 2,
    },
]

SENSITIVE_WORDS = [
    {
        "wordId": 1,
        "word": "测试敏感词",
        "scene": "chat",
        "level": "medium",
        "action": "warn",
        "enabled": True,
        "hitCount": 18,
    },
    {
        "wordId": 2,
        "word": "违规广告",
        "scene": "moment",
        "level": "high",
        "action": "block",
        "enabled": True,
        "hitCount": 7,
    },
]

ANNOUNCEMENTS = [
    {
        "announcementId": 1,
        "title": "系统维护通知",
        "scope": "all",
        "status": "published",
        "readCount": 1203,
        "createdAt": now_text(1440),
    },
    {
        "announcementId": 2,
        "title": "社区规范提醒",
        "scope": "all",
        "status": "draft",
        "readCount": 0,
        "createdAt": now_text(240),
    },
]

AUDIT_LOGS = [
    {
        "logId": 1,
        "adminName": "平台管理员",
        "module": "用户管理",
        "action": "冻结用户",
        "target": "risk@example.com",
        "ip": "127.0.0.1",
        "createdAt": now_text(25),
    },
    {
        "logId": 2,
        "adminName": "平台管理员",
        "module": "敏感词",
        "action": "新增词条",
        "target": "违规广告",
        "ip": "127.0.0.1",
        "createdAt": now_text(46),
    },
]


def trend_points() -> list[dict]:
    points = []
    for index in range(12):
        points.append(
            {
                "label": f"{index * 2:02d}:00",
                "onlineUsers": 120 + index * 9,
                "messages": 2300 + index * 180,
                "sensitiveHits": 2 + index % 4,
            }
        )
    return points

