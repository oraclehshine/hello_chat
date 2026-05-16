class GroupSummary {
  const GroupSummary({
    required this.groupId,
    required this.groupName,
    this.description,
    this.avatarUrl,
    this.notice,
    this.ownerId,
    this.inviteCode,
    this.chatEnabled = true,
    this.noticeUnread = false,
    required this.memberCount,
    required this.unreadCount,
    this.mentionUnreadCount = 0,
  });

  final int groupId;
  final String groupName;
  final String? description;
  final String? avatarUrl;
  final String? notice;
  final int? ownerId;
  final String? inviteCode;
  final bool chatEnabled;
  final bool noticeUnread;
  final int memberCount;
  final int unreadCount;
  final int mentionUnreadCount;

  factory GroupSummary.fromJson(Map<String, dynamic> json) {
    return GroupSummary(
      groupId: (json['groupId'] as num?)?.toInt() ?? 0,
      groupName: json['groupName']?.toString() ?? '',
      description: json['description']?.toString(),
      avatarUrl: json['avatarUrl']?.toString(),
      notice: json['notice']?.toString(),
      ownerId: (json['ownerId'] as num?)?.toInt(),
      inviteCode: json['inviteCode']?.toString(),
      chatEnabled: _boolish(json['chatEnabled'], defaultValue: true),
      noticeUnread: _boolish(json['noticeUnread']),
      memberCount: (json['memberCount'] as num?)?.toInt() ?? 0,
      unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
      mentionUnreadCount: (json['mentionUnreadCount'] as num?)?.toInt() ?? 0,
    );
  }

  static bool _boolish(Object? value, {bool defaultValue = false}) {
    if (value == null) return defaultValue;
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) return value == '1' || value.toLowerCase() == 'true';
    return defaultValue;
  }
}

class GroupMessage {
  const GroupMessage({
    required this.messageId,
    required this.groupId,
    required this.senderId,
    required this.senderNickname,
    this.senderAvatarUrl,
    required this.messageType,
    required this.content,
    this.fileId,
    this.replyToMessageId,
    this.replyPreview,
    this.mentionUserIds = const <int>[],
    this.mentionAll = false,
    this.fileName,
    this.fileMimeType,
    this.fileSize,
    required this.sentAt,
    required this.recallStatus,
  });

  final int messageId;
  final int groupId;
  final int senderId;
  final String senderNickname;
  final String? senderAvatarUrl;
  final String messageType;
  final String content;
  final int? fileId;
  final int? replyToMessageId;
  final String? replyPreview;
  final List<int> mentionUserIds;
  final bool mentionAll;
  final String? fileName;
  final String? fileMimeType;
  final int? fileSize;
  final String sentAt;
  final int recallStatus;

  factory GroupMessage.fromJson(Map<String, dynamic> json) {
    return GroupMessage(
      messageId: (json['messageId'] as num?)?.toInt() ?? 0,
      groupId: (json['groupId'] as num?)?.toInt() ?? 0,
      senderId: (json['senderId'] as num?)?.toInt() ?? 0,
      senderNickname: json['senderNickname']?.toString() ?? '',
      senderAvatarUrl: json['senderAvatarUrl']?.toString(),
      messageType: json['messageType']?.toString() ?? 'text',
      content: json['content']?.toString() ?? '',
      fileId: (json['fileId'] as num?)?.toInt(),
      replyToMessageId: (json['replyToMessageId'] as num?)?.toInt(),
      replyPreview: json['replyPreview']?.toString(),
      mentionUserIds: (json['mentionUserIds'] as List<dynamic>? ?? const [])
          .whereType<num>()
          .map((item) => item.toInt())
          .toList(),
      mentionAll: _boolish(json['mentionAll']),
      fileName: json['fileName']?.toString(),
      fileMimeType: json['fileMimeType']?.toString(),
      fileSize: (json['fileSize'] as num?)?.toInt(),
      sentAt: json['sentAt']?.toString() ?? '',
      recallStatus: (json['recallStatus'] as num?)?.toInt() ?? 0,
    );
  }

  static bool _boolish(Object? value) {
    if (value is bool) return value;
    if (value is num) return value != 0;
    if (value is String) return value == '1' || value.toLowerCase() == 'true';
    return false;
  }
}

class GroupJoinRequestItem {
  const GroupJoinRequestItem({
    required this.requestId,
    required this.groupId,
    required this.requesterId,
    required this.requesterNickname,
    this.requesterAvatarUrl,
    this.requesterEmail,
    required this.status,
    this.message,
    this.createdAt,
  });

  final int requestId;
  final int groupId;
  final int requesterId;
  final String requesterNickname;
  final String? requesterAvatarUrl;
  final String? requesterEmail;
  final int status;
  final String? message;
  final String? createdAt;

  factory GroupJoinRequestItem.fromJson(Map<String, dynamic> json) {
    return GroupJoinRequestItem(
      requestId: (json['requestId'] as num?)?.toInt() ?? 0,
      groupId: (json['groupId'] as num?)?.toInt() ?? 0,
      requesterId: (json['requesterId'] as num?)?.toInt() ?? 0,
      requesterNickname: json['requesterNickname']?.toString() ?? '',
      requesterAvatarUrl: json['requesterAvatarUrl']?.toString(),
      requesterEmail: json['requesterEmail']?.toString(),
      status: (json['status'] as num?)?.toInt() ?? 0,
      message: json['message']?.toString(),
      createdAt: json['createdAt']?.toString(),
    );
  }
}

class GroupMemberItem {
  const GroupMemberItem({
    required this.userId,
    required this.email,
    required this.nickname,
    this.avatarUrl,
    required this.role,
    this.groupNickname,
    this.muteUntil,
    this.noticeReadAt,
    this.joinedAt,
  });

  final int userId;
  final String email;
  final String nickname;
  final String? avatarUrl;
  final int role;
  final String? groupNickname;
  final String? muteUntil;
  final String? noticeReadAt;
  final String? joinedAt;

  factory GroupMemberItem.fromJson(Map<String, dynamic> json) {
    return GroupMemberItem(
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      email: json['email']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
      role: (json['role'] as num?)?.toInt() ?? 0,
      groupNickname: json['groupNickname']?.toString(),
      muteUntil: json['muteUntil']?.toString(),
      noticeReadAt: json['noticeReadAt']?.toString(),
      joinedAt: json['joinedAt']?.toString(),
    );
  }
}

class GroupNotificationItem {
  const GroupNotificationItem({
    required this.notificationId,
    required this.groupId,
    this.actorId,
    this.targetUserId,
    required this.noticeType,
    required this.content,
    required this.createdAt,
  });

  final int notificationId;
  final int groupId;
  final int? actorId;
  final int? targetUserId;
  final String noticeType;
  final String content;
  final String createdAt;

  factory GroupNotificationItem.fromJson(Map<String, dynamic> json) {
    return GroupNotificationItem(
      notificationId: (json['notificationId'] as num?)?.toInt() ?? 0,
      groupId: (json['groupId'] as num?)?.toInt() ?? 0,
      actorId: (json['actorId'] as num?)?.toInt(),
      targetUserId: (json['targetUserId'] as num?)?.toInt(),
      noticeType: json['noticeType']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class GroupNoticeReadStat {
  const GroupNoticeReadStat({
    required this.readCount,
    required this.memberCount,
  });

  final int readCount;
  final int memberCount;

  factory GroupNoticeReadStat.fromJson(Map<String, dynamic> json) {
    return GroupNoticeReadStat(
      readCount: (json['readCount'] as num?)?.toInt() ?? 0,
      memberCount: (json['memberCount'] as num?)?.toInt() ?? 0,
    );
  }
}
