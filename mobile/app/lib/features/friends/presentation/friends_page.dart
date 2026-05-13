import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/chat_summary.dart';
import 'package:app/core/models/friend.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/core/network/paged_result.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:app/shared/widgets/section_header.dart';
import 'package:flutter/material.dart';

class FriendsPage extends StatefulWidget {
  const FriendsPage({super.key, required this.onOpenChat});

  final ValueChanged<ChatSummary> onOpenChat;

  @override
  State<FriendsPage> createState() => _FriendsPageState();
}

class _FriendsPageState extends State<FriendsPage> {
  final TextEditingController _searchController = TextEditingController();
  int _tab = 0;
  bool _searching = false;
  bool _loadingMore = false;
  bool _searchHasMore = false;
  int _searchPage = 1;
  String _searchKeyword = '';
  List<FriendItem> _searchResults = const <FriendItem>[];
  late Future<_FriendPageData> _future;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _future = _load();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<_FriendPageData> _load() async {
    final scope = AppScope.of(context);
    final results = await Future.wait<dynamic>([
      scope.friendService.listFriends(),
      scope.friendService.listReceivedFriendRequests(),
      scope.friendService.listSentFriendRequests(),
    ]);
    return _FriendPageData(
      friends: results[0] as List<FriendItem>,
      received: results[1] as List<FriendRequestItem>,
      sent: results[2] as List<FriendRequestItem>,
    );
  }

  Future<void> _reload() async {
    setState(() {
      _future = _load();
    });
  }

  void _clearSearchState() {
    setState(() {
      _searchResults = const <FriendItem>[];
      _searchKeyword = '';
      _searchPage = 1;
      _searchHasMore = false;
      _loadingMore = false;
    });
  }

  Future<void> _searchUsers() async {
    final keyword = _searchController.text.trim();
    if (keyword.isEmpty) {
      _clearSearchState();
      return;
    }

    setState(() {
      _searching = true;
    });
    try {
      final paged = await AppScope.of(
        context,
      ).chatService.searchUsers(keyword: keyword, page: 1, pageSize: 10);
      if (!mounted) return;
      setState(() {
        _searchResults = paged.list;
        _searchKeyword = keyword;
        _searchPage = paged.page;
        _searchHasMore = paged.hasMore;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _searching = false;
        });
      }
    }
  }

  Future<void> _loadMoreSearchResults() async {
    if (_loadingMore || !_searchHasMore || _searchKeyword.isEmpty) return;
    setState(() {
      _loadingMore = true;
    });
    try {
      final PagedResult<FriendItem> paged = await AppScope.of(context)
          .chatService
          .searchUsers(
            keyword: _searchKeyword,
            page: _searchPage + 1,
            pageSize: 10,
          );
      if (!mounted) return;
      setState(() {
        _searchResults = <FriendItem>[..._searchResults, ...paged.list];
        _searchPage = paged.page;
        _searchHasMore = paged.hasMore;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _loadingMore = false;
        });
      }
    }
  }

  Future<void> _sendFriendRequest(FriendItem item) async {
    try {
      await AppScope.of(context).friendService.sendFriendRequest(
        receiverId: item.userId,
        remark: '你好，我想加你为好友',
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('已向 ${item.nickname} 发送好友申请')));
      await _reload();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _openPrivateChat(FriendItem item) async {
    try {
      final chat = await AppScope.of(
        context,
      ).chatService.createPrivateChat(targetUserId: item.userId);
      widget.onOpenChat(chat);
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _handleFriendRequest({
    required int requestId,
    required bool approve,
  }) async {
    try {
      final service = AppScope.of(context).friendService;
      if (approve) {
        await service.approveFriendRequest(requestId);
      } else {
        await service.rejectFriendRequest(requestId);
      }
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(approve ? '已通过好友申请' : '已拒绝好友申请')));
      await _reload();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    const tabs = ['好友', '收到', '已发送'];

    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 84, 20, 120),
        children: [
          SectionHeader(
            title: '好友',
            action: IconButton(
              onPressed: _reload,
              icon: const Icon(Icons.refresh_rounded),
            ),
          ),
          const SizedBox(height: 18),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchController,
                  textInputAction: TextInputAction.search,
                  onSubmitted: (_) => _searchUsers(),
                  decoration: InputDecoration(
                    hintText: '搜索用户昵称或邮箱',
                    prefixIcon: const Icon(Icons.person_search_rounded),
                    suffixIcon: _searchController.text.isEmpty
                        ? null
                        : IconButton(
                            onPressed: () {
                              _searchController.clear();
                              _clearSearchState();
                            },
                            icon: const Icon(Icons.close_rounded),
                          ),
                  ),
                  onChanged: (_) => setState(() {}),
                ),
              ),
              const SizedBox(width: 12),
              FilledButton(
                onPressed: _searching ? null : _searchUsers,
                child: Text(_searching ? '搜索中' : '搜索'),
              ),
            ],
          ),
          if (_searchResults.isNotEmpty) ...[
            const SizedBox(height: 18),
            const Text(
              '搜索结果',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: AppTheme.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            FutureBuilder<_FriendPageData>(
              future: _future,
              builder: (context, snapshot) {
                final friends = snapshot.data?.friends ?? const <FriendItem>[];
                final sent = snapshot.data?.sent ?? const <FriendRequestItem>[];
                final friendIds = friends.map((item) => item.userId).toSet();
                final pendingRequestUserIds = sent
                    .where((item) => item.status == 0)
                    .map((item) => item.receiverId)
                    .toSet();
                return Column(
                  children: [
                    ..._searchResults.map(
                      (item) => Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: GlassCard(
                          child: Row(
                            children: [
                              AppAvatar(
                                label: item.nickname,
                                size: 52,
                                imageUrl: item.avatarUrl,
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      item.nickname,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w800,
                                        color: AppTheme.textPrimary,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      item.signature?.isNotEmpty == true
                                          ? item.signature!
                                          : item.email,
                                    ),
                                  ],
                                ),
                              ),
                              friendIds.contains(item.userId)
                                  ? OutlinedButton(
                                      onPressed: () => _openPrivateChat(item),
                                      child: const Text('发消息'),
                                    )
                                  : pendingRequestUserIds.contains(item.userId)
                                  ? FilledButton.tonal(
                                      onPressed: null,
                                      child: const Text('已申请'),
                                    )
                                  : FilledButton.tonal(
                                      onPressed: () => _sendFriendRequest(item),
                                      child: const Text('加好友'),
                                    ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    if (_searchHasMore)
                      Padding(
                        padding: const EdgeInsets.only(top: 6, bottom: 8),
                        child: Center(
                          child: FilledButton.tonal(
                            onPressed: _loadingMore
                                ? null
                                : _loadMoreSearchResults,
                            child: Text(_loadingMore ? '加载中...' : '加载更多'),
                          ),
                        ),
                      ),
                  ],
                );
              },
            ),
          ],
          const SizedBox(height: 18),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: List.generate(
                tabs.length,
                (index) => Padding(
                  padding: EdgeInsets.only(
                    right: index == tabs.length - 1 ? 0 : 10,
                  ),
                  child: ChoiceChip(
                    label: Text(tabs[index]),
                    selected: _tab == index,
                    onSelected: (_) {
                      setState(() {
                        _tab = index;
                      });
                    },
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 18),
          FutureBuilder<_FriendPageData>(
            future: _future,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return GlassCard(child: Text(snapshot.error.toString()));
              }

              final data = snapshot.data!;
              if (_tab == 0) {
                return _buildFriendList(data.friends);
              }
              if (_tab == 1) {
                return _buildReceivedList(data.received);
              }
              return _buildSentList(data.sent);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildFriendList(List<FriendItem> items) {
    if (items.isEmpty) {
      return const GlassCard(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: Text('当前暂无好友。'),
        ),
      );
    }

    return Column(
      children: items
          .map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: GlassCard(
                child: Row(
                  children: [
                    AppAvatar(
                      label: item.nickname,
                      size: 54,
                      imageUrl: item.avatarUrl,
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.remarkName?.isNotEmpty == true
                                ? item.remarkName!
                                : item.nickname,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            item.signature?.isNotEmpty == true
                                ? item.signature!
                                : item.email,
                          ),
                        ],
                      ),
                    ),
                    FilledButton.tonal(
                      onPressed: () => _openPrivateChat(item),
                      child: const Text('私聊'),
                    ),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }

  Widget _buildReceivedList(List<FriendRequestItem> items) {
    if (items.isEmpty) {
      return const GlassCard(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: Text('当前没有待处理好友申请。'),
        ),
      );
    }

    return Column(
      children: items
          .map(
            (request) => Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: GlassCard(
                child: Column(
                  children: [
                    Row(
                      children: [
                        AppAvatar(
                          label: request.requesterNickname,
                          size: 54,
                          imageUrl: request.requesterAvatarUrl,
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                request.requesterNickname,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w800,
                                  color: AppTheme.textPrimary,
                                ),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                request.remark?.isNotEmpty == true
                                    ? request.remark!
                                    : '发来了一条好友申请',
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton(
                            onPressed: () => _handleFriendRequest(
                              requestId: request.requestId,
                              approve: false,
                            ),
                            child: const Text('拒绝'),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: FilledButton(
                            onPressed: () => _handleFriendRequest(
                              requestId: request.requestId,
                              approve: true,
                            ),
                            child: const Text('通过'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }

  Widget _buildSentList(List<FriendRequestItem> items) {
    if (items.isEmpty) {
      return const GlassCard(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 24),
          child: Text('当前没有已发送好友申请。'),
        ),
      );
    }

    return Column(
      children: items
          .map(
            (request) => Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: GlassCard(
                child: Row(
                  children: [
                    AppAvatar(
                      label: request.receiverNickname ?? '好友',
                      size: 54,
                      imageUrl: request.receiverAvatarUrl,
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            request.receiverNickname ??
                                '用户 #${request.receiverId}',
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            request.remark?.isNotEmpty == true
                                ? request.remark!
                                : '等待对方处理',
                          ),
                        ],
                      ),
                    ),
                    Chip(label: Text(_requestStatusText(request.status))),
                  ],
                ),
              ),
            ),
          )
          .toList(),
    );
  }

  String _requestStatusText(int status) {
    switch (status) {
      case 1:
        return '已通过';
      case 2:
        return '已拒绝';
      default:
        return '待处理';
    }
  }
}

class _FriendPageData {
  const _FriendPageData({
    required this.friends,
    required this.received,
    required this.sent,
  });

  final List<FriendItem> friends;
  final List<FriendRequestItem> received;
  final List<FriendRequestItem> sent;
}
