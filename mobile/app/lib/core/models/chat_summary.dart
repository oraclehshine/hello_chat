class ChatSummary {
  const ChatSummary({
    required this.chatId,
    required this.targetUserId,
    required this.targetEmail,
    required this.targetNickname,
    this.targetAvatarUrl,
    this.lastMessagePreview,
    this.lastMessageAt,
    required this.unreadCount,
  });

  final int chatId;
  final int targetUserId;
  final String targetEmail;
  final String targetNickname;
  final String? targetAvatarUrl;
  final String? lastMessagePreview;
  final String? lastMessageAt;
  final int unreadCount;

  factory ChatSummary.fromJson(Map<String, dynamic> json) {
    return ChatSummary(
      chatId: (json['chatId'] as num?)?.toInt() ?? 0,
      targetUserId: (json['targetUserId'] as num?)?.toInt() ?? 0,
      targetEmail: json['targetEmail']?.toString() ?? '',
      targetNickname: json['targetNickname']?.toString() ?? '',
      targetAvatarUrl: json['targetAvatarUrl']?.toString(),
      lastMessagePreview: json['lastMessagePreview']?.toString(),
      lastMessageAt: json['lastMessageAt']?.toString(),
      unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
    );
  }
}
