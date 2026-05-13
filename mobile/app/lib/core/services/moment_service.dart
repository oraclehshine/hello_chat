import 'package:app/core/models/moment.dart';
import 'package:app/core/network/api_client.dart';
import 'package:app/core/network/paged_result.dart';

class MomentService {
  MomentService(this._client);

  final ApiClient _client;

  Future<PagedResult<Moment>> listMoments({
    int page = 1,
    int pageSize = 20,
  }) async {
    final data = await _client.get(
      '/moments',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return PagedResult.fromJson(data, Moment.fromJson);
  }

  Future<PagedResult<Moment>> listCollectedMoments({
    int page = 1,
    int pageSize = 20,
  }) async {
    final data = await _client.get(
      '/moments/collections',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return PagedResult.fromJson(data, Moment.fromJson);
  }

  Future<PagedResult<Moment>> listUserMoments({
    required int userId,
    int page = 1,
    int pageSize = 20,
  }) async {
    final data = await _client.get(
      '/moments/users/$userId',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return PagedResult.fromJson(data, Moment.fromJson);
  }

  Future<MomentProfileSummary> getProfileSummary(int userId) async {
    final data = await _client.get('/moments/users/$userId/summary');
    return MomentProfileSummary.fromJson(data);
  }

  Future<Moment> createMoment({
    required String content,
    String? mood,
    String? activity,
    String? location,
    List<int> fileIds = const <int>[],
  }) async {
    final data = await _client.post(
      '/moments',
      data: {
        'content': content,
        'fileIds': fileIds,
        'tags': <String>[],
        'visibility': 'public',
        'visibleUserIds': <int>[],
        if (mood != null && mood.trim().isNotEmpty) 'mood': mood.trim(),
        if (activity != null && activity.trim().isNotEmpty)
          'activity': activity.trim(),
        if (location != null && location.trim().isNotEmpty)
          'location': location.trim(),
      },
    );
    return Moment.fromJson(data);
  }

  Future<Moment> likeMoment(int momentId) async {
    final data = await _client.post('/moments/$momentId/likes');
    return Moment.fromJson(data);
  }

  Future<Moment> unlikeMoment(int momentId) async {
    final data = await _client.delete('/moments/$momentId/likes');
    return Moment.fromJson(data);
  }

  Future<Moment> collectMoment(int momentId) async {
    final data = await _client.post('/moments/$momentId/collect');
    return Moment.fromJson(data);
  }

  Future<Moment> uncollectMoment(int momentId) async {
    final data = await _client.delete('/moments/$momentId/collect');
    return Moment.fromJson(data);
  }

  Future<void> deleteMoment(int momentId) async {
    await _client.delete('/moments/$momentId');
  }

  Future<void> reportMoment({
    required int momentId,
    required String reason,
  }) async {
    await _client.post('/moments/$momentId/report', data: {'reason': reason});
  }

  Future<List<MomentComment>> listComments(int momentId) async {
    final data = await _client.get('/moments/$momentId/comments');
    final raw = (data['_value'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();
    return raw.map(MomentComment.fromJson).toList();
  }

  Future<void> deleteComment({
    required int momentId,
    required int commentId,
  }) async {
    await _client.delete('/moments/$momentId/comments/$commentId');
  }

  Future<PagedResult<MomentNotification>> listNotifications({
    int page = 1,
    int pageSize = 20,
  }) async {
    final data = await _client.get(
      '/moments/notifications',
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
    return PagedResult.fromJson(data, MomentNotification.fromJson);
  }

  Future<int> countUnreadNotifications() async {
    final data = await _client.get('/moments/notifications/unread-count');
    return (data['_value'] as num?)?.toInt() ?? 0;
  }

  Future<void> markAllNotificationsRead() async {
    await _client.put('/moments/notifications/read-all');
  }

  Future<void> markNotificationRead(int notificationId) async {
    await _client.put('/moments/notifications/$notificationId/read');
  }

  Future<MomentComment> addComment({
    required int momentId,
    required String content,
    int? replyToCommentId,
  }) async {
    final data = await _client.post(
      '/moments/$momentId/comments',
      data: {
        'content': content,
        ...?replyToCommentId == null
            ? null
            : {'replyToCommentId': replyToCommentId},
        'mentionUserIds': <int>[],
      },
    );
    return MomentComment.fromJson(data);
  }
}
