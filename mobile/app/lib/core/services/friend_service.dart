// ignore_for_file: use_null_aware_elements

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

  Future<FriendItem> updateFriend({
    required int friendUserId,
    String? remarkName,
    String? friendGroup,
    bool? star,
  }) async {
    final data = await _client.put(
      '/users/friends/$friendUserId',
      data: {
        if (remarkName != null) 'remarkName': remarkName.trim(),
        if (friendGroup != null) 'friendGroup': friendGroup.trim(),
        if (star != null) 'star': star,
      },
    );
    return FriendItem.fromJson(data);
  }

  Future<void> deleteFriend(int friendUserId) async {
    await _client.delete('/users/friends/$friendUserId');
  }

  Future<void> blockUser(int blockedUserId) async {
    await _client.post('/users/blocks/$blockedUserId');
  }

  Future<void> unblockUser(int blockedUserId) async {
    await _client.delete('/users/blocks/$blockedUserId');
  }

  Future<List<BlockedUserItem>> listBlockedUsers() async {
    final data = await _client.get('/users/blocks');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(BlockedUserItem.fromJson).toList();
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
