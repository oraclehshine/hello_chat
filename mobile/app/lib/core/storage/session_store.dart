import 'dart:convert';

import 'package:app/core/models/auth_session.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SessionStore extends ChangeNotifier {
  static const _sessionKey = 'hello_chat_mobile_session';

  AuthSession? _session;

  AuthSession? get session => _session;
  String? get accessToken => _session?.accessToken;

  Future<void> load() async {
    final preferences = await SharedPreferences.getInstance();
    final raw = preferences.getString(_sessionKey);
    if (raw == null || raw.isEmpty) {
      _session = null;
      notifyListeners();
      return;
    }

    _session = AuthSession.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    notifyListeners();
  }

  Future<void> save(AuthSession session) async {
    _session = session;
    final preferences = await SharedPreferences.getInstance();
    await preferences.setString(_sessionKey, jsonEncode(session.toJson()));
    notifyListeners();
  }

  Future<void> clear() async {
    _session = null;
    final preferences = await SharedPreferences.getInstance();
    await preferences.remove(_sessionKey);
    notifyListeners();
  }
}
