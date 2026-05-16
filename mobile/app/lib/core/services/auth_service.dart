import 'package:app/core/models/auth_session.dart';
import 'package:app/core/models/user_profile.dart';
import 'package:app/core/network/api_client.dart';
import 'package:app/core/storage/session_store.dart';

class AuthService {
  AuthService(this._client, this._sessionStore);

  final ApiClient _client;
  final SessionStore _sessionStore;

  Future<String> sendCaptcha(String email, String scene) async {
    final data = await _client.post(
      '/auth/email-captcha',
      data: {'email': email, 'scene': scene},
    );
    return data['_value']?.toString() ?? '';
  }

  Future<AuthSession> register({
    required String email,
    required String password,
    required String captcha,
  }) async {
    final data = await _client.post(
      '/auth/register',
      data: {'email': email, 'password': password, 'captcha': captcha},
    );
    final session = AuthSession.fromJson(data);
    await _sessionStore.save(session);
    return session;
  }

  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final data = await _client.post(
      '/auth/login',
      data: {'email': email, 'password': password},
    );
    final session = AuthSession.fromJson(data);
    await _sessionStore.save(session);
    return session;
  }

  Future<void> resetPassword({
    required String email,
    required String captcha,
    required String newPassword,
  }) async {
    await _client.post(
      '/auth/password-reset',
      data: {'email': email, 'captcha': captcha, 'newPassword': newPassword},
    );
  }

  Future<void> changePassword({
    required String oldPassword,
    required String newPassword,
  }) async {
    await _client.put(
      '/auth/password',
      data: {'oldPassword': oldPassword, 'newPassword': newPassword},
    );
  }

  Future<UserProfile> updateEmail({
    required String email,
    required String captcha,
  }) async {
    final data = await _client.put(
      '/users/me/email',
      data: {'email': email, 'captcha': captcha},
    );
    return UserProfile.fromJson(data);
  }

  Future<void> logout() async {
    final refreshToken = _sessionStore.session?.refreshToken;
    if (refreshToken != null && refreshToken.isNotEmpty) {
      await _client.post('/auth/logout', data: {'refreshToken': refreshToken});
    }
    await _sessionStore.clear();
  }

  Future<UserProfile> getMe() async {
    final data = await _client.get('/users/me');
    return UserProfile.fromJson(data);
  }

  Future<UserProfile> updateProfile({
    String? nickname,
    String? signature,
    String? phone,
    String? avatarUrl,
  }) async {
    final data = await _client.put(
      '/users/me',
      data: {
        ...?nickname == null ? null : {'nickname': nickname},
        ...?signature == null ? null : {'signature': signature},
        ...?phone == null ? null : {'phone': phone},
        ...?avatarUrl == null ? null : {'avatarUrl': avatarUrl},
      },
    );
    return UserProfile.fromJson(data);
  }
}
