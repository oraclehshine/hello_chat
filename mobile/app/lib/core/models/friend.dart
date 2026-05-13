class FriendItem {
  const FriendItem({
    required this.userId,
    required this.email,
    required this.nickname,
    this.avatarUrl,
    this.signature,
    this.remarkName,
  });

  final int userId;
  final String email;
  final String nickname;
  final String? avatarUrl;
  final String? signature;
  final String? remarkName;

  factory FriendItem.fromJson(Map<String, dynamic> json) {
    return FriendItem(
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      email: json['email']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
      signature: json['signature']?.toString(),
      remarkName: json['remarkName']?.toString(),
    );
  }
}

class FriendRequestItem {
  const FriendRequestItem({
    required this.requestId,
    required this.requesterId,
    required this.requesterNickname,
    this.requesterAvatarUrl,
    this.requesterEmail,
    this.receiverId,
    this.receiverNickname,
    this.receiverAvatarUrl,
    this.receiverEmail,
    this.remark,
    required this.status,
    this.createdAt,
  });

  final int requestId;
  final int requesterId;
  final String requesterNickname;
  final String? requesterAvatarUrl;
  final String? requesterEmail;
  final int? receiverId;
  final String? receiverNickname;
  final String? receiverAvatarUrl;
  final String? receiverEmail;
  final String? remark;
  final int status;
  final String? createdAt;

  factory FriendRequestItem.fromJson(Map<String, dynamic> json) {
    return FriendRequestItem(
      requestId: (json['requestId'] as num?)?.toInt() ?? 0,
      requesterId: (json['requesterId'] as num?)?.toInt() ?? 0,
      requesterNickname: json['requesterNickname']?.toString() ?? '',
      requesterAvatarUrl: json['requesterAvatarUrl']?.toString(),
      requesterEmail: json['requesterEmail']?.toString(),
      receiverId: (json['receiverId'] as num?)?.toInt(),
      receiverNickname: json['receiverNickname']?.toString(),
      receiverAvatarUrl: json['receiverAvatarUrl']?.toString(),
      receiverEmail: json['receiverEmail']?.toString(),
      remark: json['remark']?.toString(),
      status: (json['status'] as num?)?.toInt() ?? 0,
      createdAt: json['createdAt']?.toString(),
    );
  }
}
