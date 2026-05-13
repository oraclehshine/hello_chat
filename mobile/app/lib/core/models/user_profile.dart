class UserProfile {
  const UserProfile({
    required this.userId,
    required this.email,
    required this.nickname,
    this.avatarUrl,
    this.signature,
    this.gender,
    this.age,
    this.phone,
    this.status,
  });

  final int userId;
  final String email;
  final String nickname;
  final String? avatarUrl;
  final String? signature;
  final int? gender;
  final int? age;
  final String? phone;
  final String? status;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      email: json['email']?.toString() ?? '',
      nickname: json['nickname']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
      signature: json['signature']?.toString(),
      gender: (json['gender'] as num?)?.toInt(),
      age: (json['age'] as num?)?.toInt(),
      phone: json['phone']?.toString(),
      status: json['status']?.toString(),
    );
  }
}
