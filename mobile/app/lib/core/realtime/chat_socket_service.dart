import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:app/core/config/app_environment.dart';
import 'package:app/core/storage/session_store.dart';

class ChatSocketEvent {
  const ChatSocketEvent({
    required this.eventType,
    required this.userId,
    required this.payload,
  });

  final String eventType;
  final int userId;
  final Map<String, dynamic> payload;

  factory ChatSocketEvent.fromJson(Map<String, dynamic> json) {
    return ChatSocketEvent(
      eventType: json['eventType']?.toString() ?? '',
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      payload: (json['payload'] as Map?)?.cast<String, dynamic>() ?? const <String, dynamic>{},
    );
  }
}

class ChatSocketService {
  ChatSocketService(this._sessionStore);

  final SessionStore _sessionStore;
  final StreamController<ChatSocketEvent> _events =
      StreamController<ChatSocketEvent>.broadcast();

  WebSocket? _socket;
  Timer? _reconnectTimer;
  bool _connecting = false;
  bool _closedByUser = false;

  Stream<ChatSocketEvent> get events => _events.stream;

  Future<void> ensureConnected() async {
    if (_closedByUser) {
      _closedByUser = false;
    }
    if (_connecting || _socket != null) {
      return;
    }
    final token = _sessionStore.accessToken;
    if (token == null || token.isEmpty) {
      return;
    }

    _connecting = true;
    try {
      final socket = await WebSocket.connect(
        '${AppEnvironment.webSocketUrl}?token=$token',
      );
      _socket = socket;
      socket.listen(
        _handleData,
        onDone: _handleDisconnect,
        onError: (_) => _handleDisconnect(),
        cancelOnError: true,
      );
    } catch (_) {
      _scheduleReconnect();
    } finally {
      _connecting = false;
    }
  }

  Future<void> disconnect() async {
    _closedByUser = true;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
    final socket = _socket;
    _socket = null;
    await socket?.close();
  }

  void _handleData(dynamic raw) {
    try {
      final decoded = jsonDecode(raw.toString()) as Map<String, dynamic>;
      _events.add(ChatSocketEvent.fromJson(decoded));
    } catch (_) {
      // Ignore malformed payloads to keep the realtime channel resilient.
    }
  }

  void _handleDisconnect() {
    _socket = null;
    if (_closedByUser) {
      return;
    }
    _scheduleReconnect();
  }

  void _scheduleReconnect() {
    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(const Duration(seconds: 3), () {
      ensureConnected();
    });
  }
}
