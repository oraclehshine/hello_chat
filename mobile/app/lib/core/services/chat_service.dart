import 'package:app/core/models/chat_message.dart';
import 'package:app/core/models/chat_summary.dart';
import 'package:app/core/models/friend.dart';
import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/paged_result.dart';

class ChatService {
  ChatService(this._client);

  final ApiClient _client;

  Future<List<ChatSummary>> listChats() async {
    final data = await _client.get('/chats');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(ChatSummary.fromJson).toList();
  }

  Future<ChatSummary> createPrivateChat({required int targetUserId}) async {
    final data = await _client.post(
      '/chats/private',
      data: {'targetUserId': targetUserId},
    );
    return ChatSummary.fromJson(data);
  }

  Future<PagedResult<ChatMessage>> listMessages(int chatId) async {
    final data = await _client.get(
      '/chats/$chatId/messages',
      queryParameters: {'page': 1, 'pageSize': 50},
    );
    return PagedResult.fromJson(data, ChatMessage.fromJson);
  }

  Future<ChatMessage> sendMessage({
    required int chatId,
    required String content,
  }) async {
    final data = await _client.post(
      '/chats/$chatId/messages',
      data: {'messageType': 'text', 'content': content},
    );
    return ChatMessage.fromJson(data);
  }

  Future<ChatMessage> sendAttachment({
    required int chatId,
    required int fileId,
    required String fileName,
    required int fileSize,
    required bool isImage,
  }) async {
    final data = await _client.post(
      '/chats/$chatId/messages',
      data: {
        'messageType': isImage ? 'image' : 'file',
        'content': '',
        'fileId': fileId,
        'fileName': fileName,
        'fileSize': fileSize,
      },
    );
    return ChatMessage.fromJson(data);
  }

  Future<void> markChatAsRead(int chatId) async {
    await _client.post('/chats/$chatId/read');
  }

  Future<void> updateTypingStatus({
    required int chatId,
    required bool typing,
  }) async {
    await _client.post('/chats/$chatId/typing', data: {'typing': typing});
  }

  Future<void> recallMessage(int messageId) async {
    await _client.post('/messages/$messageId/recall');
  }

  Future<void> deleteMessage(int messageId) async {
    await _client.delete('/messages/$messageId');
  }

  Future<void> pinMessage(int messageId) async {
    await _client.post('/messages/$messageId/pin');
  }

  Future<void> unpinMessage(int messageId) async {
    await _client.delete('/messages/$messageId/pin');
  }

  Future<PagedResult<ChatMessage>> searchMessages({
    required int chatId,
    required String keyword,
    int page = 1,
    int pageSize = 20,
  }) async {
    final data = await _client.get(
      '/messages/search',
      queryParameters: {
        'chatId': chatId,
        'keyword': keyword,
        'page': page,
        'pageSize': pageSize,
      },
    );
    return PagedResult.fromJson(data, ChatMessage.fromJson);
  }

  Future<PagedResult<FriendItem>> searchUsers({
    required String keyword,
    int page = 1,
    int pageSize = 10,
  }) async {
    final data = await _client.get(
      '/users/search',
      queryParameters: {'keyword': keyword, 'page': page, 'pageSize': pageSize},
    );
    return PagedResult.fromJson(data, FriendItem.fromJson);
  }
}
