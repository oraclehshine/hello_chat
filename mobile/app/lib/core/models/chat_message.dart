class ChatMessage {
  const ChatMessage({
    required this.messageId,
    required this.chatId,
    required this.senderId,
    required this.messageType,
    required this.content,
    this.fileId,
    this.fileName,
    this.fileMimeType,
    this.fileSize,
    required this.recallStatus,
    required this.messageStatus,
    required this.sentAt,
    this.pinnedAt,
  });

  final int messageId;
  final int chatId;
  final int senderId;
  final String messageType;
  final String content;
  final int? fileId;
  final String? fileName;
  final String? fileMimeType;
  final int? fileSize;
  final int recallStatus;
  final int messageStatus;
  final String sentAt;
  final String? pinnedAt;

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      messageId: (json['messageId'] as num?)?.toInt() ?? 0,
      chatId: (json['chatId'] as num?)?.toInt() ?? 0,
      senderId: (json['senderId'] as num?)?.toInt() ?? 0,
      messageType: json['messageType']?.toString() ?? 'text',
      content: json['content']?.toString() ?? '',
      fileId: (json['fileId'] as num?)?.toInt(),
      fileName: json['fileName']?.toString(),
      fileMimeType: json['fileMimeType']?.toString(),
      fileSize: (json['fileSize'] as num?)?.toInt(),
      recallStatus: (json['recallStatus'] as num?)?.toInt() ?? 0,
      messageStatus: (json['messageStatus'] as num?)?.toInt() ?? 0,
      sentAt: json['sentAt']?.toString() ?? '',
      pinnedAt: json['pinnedAt']?.toString(),
    );
  }
}
