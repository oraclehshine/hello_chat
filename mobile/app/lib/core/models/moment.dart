class Moment {
  const Moment({
    required this.momentId,
    required this.authorId,
    required this.authorNickname,
    this.authorAvatarUrl,
    required this.content,
    this.location,
    this.mood,
    this.activity,
    this.tags = const <String>[],
    this.mediaList = const <MomentMedia>[],
    required this.likeCount,
    required this.commentCount,
    required this.collectCount,
    required this.liked,
    required this.collected,
    required this.createdAt,
  });

  final int momentId;
  final int authorId;
  final String authorNickname;
  final String? authorAvatarUrl;
  final String content;
  final String? location;
  final String? mood;
  final String? activity;
  final List<String> tags;
  final List<MomentMedia> mediaList;
  final int likeCount;
  final int commentCount;
  final int collectCount;
  final bool liked;
  final bool collected;
  final String createdAt;

  factory Moment.fromJson(Map<String, dynamic> json) {
    return Moment(
      momentId: (json['momentId'] as num?)?.toInt() ?? 0,
      authorId: (json['authorId'] as num?)?.toInt() ?? 0,
      authorNickname: json['authorNickname']?.toString() ?? '',
      authorAvatarUrl: json['authorAvatarUrl']?.toString(),
      content: json['content']?.toString() ?? '',
      location: json['location']?.toString(),
      mood: json['mood']?.toString(),
      activity: json['activity']?.toString(),
      tags: (json['tags'] as List<dynamic>? ?? const <dynamic>[])
          .map((item) => item.toString())
          .toList(),
      mediaList: (json['mediaList'] as List<dynamic>? ?? const <dynamic>[])
          .whereType<Map<String, dynamic>>()
          .map(MomentMedia.fromJson)
          .toList(),
      likeCount: (json['likeCount'] as num?)?.toInt() ?? 0,
      commentCount: (json['commentCount'] as num?)?.toInt() ?? 0,
      collectCount: (json['collectCount'] as num?)?.toInt() ?? 0,
      liked: json['liked'] as bool? ?? false,
      collected: json['collected'] as bool? ?? false,
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  Moment copyWith({
    int? likeCount,
    int? commentCount,
    int? collectCount,
    bool? liked,
    bool? collected,
  }) {
    return Moment(
      momentId: momentId,
      authorId: authorId,
      authorNickname: authorNickname,
      authorAvatarUrl: authorAvatarUrl,
      content: content,
      location: location,
      mood: mood,
      activity: activity,
      tags: tags,
      mediaList: mediaList,
      likeCount: likeCount ?? this.likeCount,
      commentCount: commentCount ?? this.commentCount,
      collectCount: collectCount ?? this.collectCount,
      liked: liked ?? this.liked,
      collected: collected ?? this.collected,
      createdAt: createdAt,
    );
  }
}

class MomentMedia {
  const MomentMedia({
    required this.fileId,
    required this.fileUrl,
    required this.fileName,
    required this.fileType,
    required this.mimeType,
    required this.fileSize,
    required this.sortOrder,
  });

  final int fileId;
  final String fileUrl;
  final String fileName;
  final String fileType;
  final String mimeType;
  final int fileSize;
  final int sortOrder;

  factory MomentMedia.fromJson(Map<String, dynamic> json) {
    return MomentMedia(
      fileId: (json['fileId'] as num?)?.toInt() ?? 0,
      fileUrl: json['fileUrl']?.toString() ?? '',
      fileName: json['fileName']?.toString() ?? '',
      fileType: json['fileType']?.toString() ?? '',
      mimeType: json['mimeType']?.toString() ?? '',
      fileSize: (json['fileSize'] as num?)?.toInt() ?? 0,
      sortOrder: (json['sortOrder'] as num?)?.toInt() ?? 0,
    );
  }
}

class MomentComment {
  const MomentComment({
    required this.commentId,
    required this.momentId,
    required this.userId,
    required this.userNickname,
    this.userAvatarUrl,
    this.replyToCommentId,
    required this.content,
    required this.createdAt,
  });

  final int commentId;
  final int momentId;
  final int userId;
  final String userNickname;
  final String? userAvatarUrl;
  final int? replyToCommentId;
  final String content;
  final String createdAt;

  factory MomentComment.fromJson(Map<String, dynamic> json) {
    return MomentComment(
      commentId: (json['commentId'] as num?)?.toInt() ?? 0,
      momentId: (json['momentId'] as num?)?.toInt() ?? 0,
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      userNickname: json['userNickname']?.toString() ?? '',
      userAvatarUrl: json['userAvatarUrl']?.toString(),
      replyToCommentId: (json['replyToCommentId'] as num?)?.toInt(),
      content: json['content']?.toString() ?? '',
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class MomentNotification {
  const MomentNotification({
    required this.notificationId,
    required this.notificationType,
    required this.title,
    required this.content,
    this.relatedId,
    required this.read,
    required this.createdAt,
  });

  final int notificationId;
  final String notificationType;
  final String title;
  final String content;
  final int? relatedId;
  final bool read;
  final String createdAt;

  factory MomentNotification.fromJson(Map<String, dynamic> json) {
    return MomentNotification(
      notificationId: (json['notificationId'] as num?)?.toInt() ?? 0,
      notificationType: json['notificationType']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      content: json['content']?.toString() ?? '',
      relatedId: (json['relatedId'] as num?)?.toInt(),
      read: ((json['read'] as num?)?.toInt() ?? 0) > 0,
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class MomentProfileSummary {
  const MomentProfileSummary({
    required this.userId,
    required this.nickname,
    required this.email,
    this.avatarUrl,
    required this.momentCount,
    required this.totalLikeCount,
    required this.totalCommentCount,
    required this.totalCollectCount,
    required this.friendCount,
    required this.followerCount,
    required this.followingCount,
  });

  final int userId;
  final String nickname;
  final String email;
  final String? avatarUrl;
  final int momentCount;
  final int totalLikeCount;
  final int totalCommentCount;
  final int totalCollectCount;
  final int friendCount;
  final int followerCount;
  final int followingCount;

  factory MomentProfileSummary.fromJson(Map<String, dynamic> json) {
    return MomentProfileSummary(
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      nickname: json['nickname']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
      momentCount: (json['momentCount'] as num?)?.toInt() ?? 0,
      totalLikeCount: (json['totalLikeCount'] as num?)?.toInt() ?? 0,
      totalCommentCount: (json['totalCommentCount'] as num?)?.toInt() ?? 0,
      totalCollectCount: (json['totalCollectCount'] as num?)?.toInt() ?? 0,
      friendCount: (json['friendCount'] as num?)?.toInt() ?? 0,
      followerCount: (json['followerCount'] as num?)?.toInt() ?? 0,
      followingCount: (json['followingCount'] as num?)?.toInt() ?? 0,
    );
  }
}
