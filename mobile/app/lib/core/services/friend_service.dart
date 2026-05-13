import 'package:app/core/models/friend.dart';
import 'package:app/core/network/api_client.dart';

class FriendService {
  FriendService(this._client);

  final ApiClient _client;

  Future<List<FriendItem>> listFriends() async {
    final data = await _client.get('/users/friends');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(FriendItem.fromJson).toList();
  }

  Future<List<FriendRequestItem>> listReceivedFriendRequests() async {
    final data = await _client.get('/users/friend-requests/received');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(FriendRequestItem.fromJson).toList();
  }

  Future<List<FriendRequestItem>> listSentFriendRequests() async {
    final data = await _client.get('/users/friend-requests/sent');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(FriendRequestItem.fromJson).toList();
  }

  Future<FriendRequestItem> sendFriendRequest({
    required int receiverId,
    String? remark,
  }) async {
    final data = await _client.post(
      '/users/friend-requests',
      data: {
        'receiverId': receiverId,
        if (remark != null && remark.trim().isNotEmpty) 'remark': remark.trim(),
      },
    );
    return FriendRequestItem.fromJson(data);
  }

  Future<void> approveFriendRequest(int requestId) async {
    await _client.post('/users/friend-requests/$requestId/approve');
  }

  Future<void> rejectFriendRequest(int requestId) async {
    await _client.post('/users/friend-requests/$requestId/reject');
  }
}
