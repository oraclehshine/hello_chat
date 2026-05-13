class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.refreshToken,
    required this.userId,
    required this.email,
    required this.nickname,
    this.avatarUrl,
  });

  final String accessToken;
  final String refreshToken;
  final int userId;
  final String email;
  final String nickname;
  final String? avatarUrl;

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      accessToken: json['accessToken']?.toString() ?? '',
      refreshToken: json['refreshToken']?.toString() ?? '',
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      email: json['email']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
    );
  }

  Map<String, dynamic> toJson() => {
        'accessToken': accessToken,
        'refreshToken': refreshToken,
        'userId': userId,
        'email': email,
        'nickname': nickname,
        'avatarUrl': avatarUrl,
      };
}
