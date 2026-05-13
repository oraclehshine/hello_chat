import 'package:app/core/models/group.dart';
import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/paged_result.dart';

class GroupService {
  GroupService(this._client);

  final ApiClient _client;

  Future<List<GroupSummary>> listGroups() async {
    final data = await _client.get('/groups');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupSummary.fromJson).toList();
  }

  Future<List<GroupSummary>> searchGroups(String keyword) async {
    final data = await _client.get(
      '/groups/search',
      queryParameters: {'keyword': keyword, 'page': 1, 'pageSize': 20},
    );
    final paged = PagedResult.fromJson(data, GroupSummary.fromJson);
    return paged.list;
  }

  Future<GroupSummary> getGroup(int groupId) async {
    final data = await _client.get('/groups/$groupId');
    return GroupSummary.fromJson(data);
  }

  Future<GroupSummary> createGroup({
    required String groupName,
    String? description,
    List<int>? memberIds,
  }) async {
    final data = await _client.post(
      '/groups',
      data: {
        'groupName': groupName,
        if (description != null && description.trim().isNotEmpty)
          'description': description.trim(),
        if (memberIds != null && memberIds.isNotEmpty) 'memberIds': memberIds,
      },
    );
    return GroupSummary.fromJson(data);
  }

  Future<GroupSummary> joinByInviteCode(String inviteCode) async {
    final data = await _client.post('/groups/invite/${inviteCode.trim()}/join');
    return GroupSummary.fromJson(data);
  }

  Future<void> requestJoinGroup({required int groupId, String? message}) async {
    await _client.post(
      '/groups/$groupId/join-requests',
      data: {
        if (message != null && message.trim().isNotEmpty)
          'message': message.trim(),
      },
    );
  }

  Future<List<GroupJoinRequestItem>> listMyJoinRequests() async {
    final data = await _client.get('/groups/join-requests/me');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupJoinRequestItem.fromJson).toList();
  }

  Future<List<GroupMemberItem>> listMembers(int groupId) async {
    final data = await _client.get('/groups/$groupId/members');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupMemberItem.fromJson).toList();
  }

  Future<List<GroupMemberItem>> setAdmin({
    required int groupId,
    required int userId,
    required bool isAdmin,
  }) async {
    final data = isAdmin
        ? await _client.post('/groups/$groupId/members/$userId/admin')
        : await _client.delete('/groups/$groupId/members/$userId/admin');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupMemberItem.fromJson).toList();
  }

  Future<List<GroupMemberItem>> muteMember({
    required int groupId,
    required int userId,
    required int minutes,
  }) async {
    final data = await _client.post(
      '/groups/$groupId/members/$userId/mute',
      data: {'minutes': minutes},
    );
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupMemberItem.fromJson).toList();
  }

  Future<List<GroupMemberItem>> unmuteMember({
    required int groupId,
    required int userId,
  }) async {
    final data = await _client.delete('/groups/$groupId/members/$userId/mute');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupMemberItem.fromJson).toList();
  }

  Future<PagedResult<GroupMessage>> listMessages(int groupId) async {
    final data = await _client.get(
      '/groups/$groupId/messages',
      queryParameters: {'page': 1, 'pageSize': 50},
    );
    return PagedResult.fromJson(data, GroupMessage.fromJson);
  }

  Future<GroupMessage> sendMessage({
    required int groupId,
    required String content,
  }) async {
    final data = await _client.post(
      '/groups/$groupId/messages',
      data: {'messageType': 'text', 'content': content},
    );
    return GroupMessage.fromJson(data);
  }

  Future<GroupMessage> sendAttachment({
    required int groupId,
    required int fileId,
    required String fileName,
    required int fileSize,
    required bool isImage,
  }) async {
    final data = await _client.post(
      '/groups/$groupId/messages',
      data: {
        'messageType': isImage ? 'image' : 'file',
        'content': '',
        'fileId': fileId,
        'fileName': fileName,
        'fileSize': fileSize,
      },
    );
    return GroupMessage.fromJson(data);
  }

  Future<GroupSummary> updateNotice({
    required int groupId,
    required String notice,
  }) async {
    final data = await _client.post(
      '/groups/$groupId/announcement',
      data: {'notice': notice},
    );
    return GroupSummary.fromJson(data);
  }

  Future<void> removeMember({required int groupId, required int userId}) async {
    await _client.delete('/groups/$groupId/members/$userId');
  }

  Future<List<GroupMemberItem>> transferOwner({
    required int groupId,
    required int targetUserId,
  }) async {
    final data = await _client.post(
      '/groups/$groupId/owner',
      data: {'targetUserId': targetUserId},
    );
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(GroupMemberItem.fromJson).toList();
  }

  Future<void> leaveGroup(int groupId) async {
    await _client.post('/groups/$groupId/leave');
  }

  Future<void> dissolveGroup(int groupId) async {
    await _client.delete('/groups/$groupId');
  }

  Future<PagedResult<GroupNotificationItem>> listNotifications({
    required int groupId,
    int page = 1,
    int pageSize = 20,
  }) async {
    final data = await _client.get(
      '/groups/$groupId/notifications',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return PagedResult.fromJson(data, GroupNotificationItem.fromJson);
  }

  Future<GroupNoticeReadStat> getNoticeReadStats(int groupId) async {
    final data = await _client.get('/groups/$groupId/announcement/read-stats');
    return GroupNoticeReadStat.fromJson(data);
  }

  Future<GroupNoticeReadStat> markNoticeRead(int groupId) async {
    final data = await _client.post('/groups/$groupId/announcement/read');
    return GroupNoticeReadStat.fromJson(data);
  }
}
