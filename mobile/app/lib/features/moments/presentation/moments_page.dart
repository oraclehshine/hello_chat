import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/moment.dart';
import 'package:app/core/models/upload_asset.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/core/utils/asset_urls.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:app/shared/widgets/section_header.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

class MomentsPage extends StatefulWidget {
  const MomentsPage({super.key});

  @override
  State<MomentsPage> createState() => _MomentsPageState();
}

class _MomentsPageState extends State<MomentsPage> {
  final List<Moment> _posts = <Moment>[];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 1;
  bool _bootstrapped = false;
  int _unreadNotificationCount = 0;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_bootstrapped) return;
    _bootstrapped = true;
    _reload();
  }

  Future<void> _loadUnreadCount() async {
    try {
      final count = await AppScope.of(
        context,
      ).momentService.countUnreadNotifications();
      if (!mounted) return;
      setState(() {
        _unreadNotificationCount = count;
      });
    } catch (_) {}
  }

  Future<void> _loadMoments({required bool append}) async {
    final result = await AppScope.of(
      context,
    ).momentService.listMoments(page: append ? _page + 1 : 1, pageSize: 20);
    if (!mounted) return;
    setState(() {
      if (append) {
        _posts.addAll(result.list);
      } else {
        _posts
          ..clear()
          ..addAll(result.list);
      }
      _page = result.page;
      _hasMore = result.hasMore;
    });
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _page = 1;
      _hasMore = true;
    });
    try {
      await _loadMoments(append: false);
    } on ApiException {
      // Session may be cleared by global interceptor on 401/token-expired.
    } catch (_) {
      // Prevent unhandled crash during initial bootstrap.
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
    await _loadUnreadCount();
  }

  Future<void> _loadMore() async {
    if (_loadingMore || !_hasMore) return;
    setState(() {
      _loadingMore = true;
    });
    try {
      await _loadMoments(append: true);
    } on ApiException {
      // Keep UI stable; global interceptor handles unauthorized logout.
    } catch (_) {
      // Ignore non-critical pagination errors here.
    } finally {
      if (mounted) {
        setState(() {
          _loadingMore = false;
        });
      }
    }
  }

  Future<void> _toggleLike(Moment item) async {
    final service = AppScope.of(context).momentService;
    if (item.liked) {
      await service.unlikeMoment(item.momentId);
    } else {
      await service.likeMoment(item.momentId);
    }
    await _reload();
  }

  Future<void> _toggleCollect(Moment item) async {
    final service = AppScope.of(context).momentService;
    if (item.collected) {
      await service.uncollectMoment(item.momentId);
    } else {
      await service.collectMoment(item.momentId);
    }
    await _reload();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton(
        onPressed: _showComposer,
        child: const Icon(Icons.add_rounded),
      ),
      body: RefreshIndicator(
        onRefresh: _reload,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 84, 20, 120),
          children: [
            SectionHeader(
              title: '朋友圈',
              subtitle: '时间线、点赞、收藏、评论、通知',
              action: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    onPressed: _showCollections,
                    icon: const Icon(Icons.bookmark_border_rounded),
                  ),
                  Stack(
                    clipBehavior: Clip.none,
                    children: [
                      IconButton(
                        onPressed: _showNotifications,
                        icon: const Icon(Icons.notifications_none_rounded),
                      ),
                      if (_unreadNotificationCount > 0)
                        Positioned(
                          top: 6,
                          right: 6,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6,
                              vertical: 2,
                            ),
                            decoration: const BoxDecoration(
                              color: Color(0xFFF15B5B),
                              borderRadius: BorderRadius.all(
                                Radius.circular(10),
                              ),
                            ),
                            child: Text(
                              _unreadNotificationCount > 99
                                  ? '99+'
                                  : '$_unreadNotificationCount',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 10,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            if (_loading)
              const Center(child: CircularProgressIndicator())
            else if (_posts.isEmpty)
              const GlassCard(child: Text('当前还没有朋友圈动态。'))
            else
              Column(
                children: [
                  ..._posts.map(
                    (item) => Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: GlassCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                AppAvatar(
                                  label: item.authorNickname,
                                  size: 46,
                                  imageUrl: item.authorAvatarUrl,
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        item.authorNickname,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w800,
                                        ),
                                      ),
                                      Text(
                                        _formatTime(item.createdAt),
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 10),
                            if (item.content.trim().isNotEmpty)
                              Text(item.content),
                            if (item.mediaList.isNotEmpty) ...[
                              const SizedBox(height: 10),
                              Wrap(
                                spacing: 8,
                                runSpacing: 8,
                                children: item.mediaList
                                    .map(
                                      (m) => ClipRRect(
                                        borderRadius: BorderRadius.circular(12),
                                        child: Image.network(
                                          resolveAssetUrl(m.fileUrl),
                                          width: 128,
                                          height: 128,
                                          fit: BoxFit.cover,
                                        ),
                                      ),
                                    )
                                    .toList(),
                              ),
                            ],
                            const SizedBox(height: 10),
                            Row(
                              children: [
                                TextButton.icon(
                                  onPressed: () => _toggleLike(item),
                                  icon: Icon(
                                    item.liked
                                        ? Icons.favorite_rounded
                                        : Icons.favorite_border_rounded,
                                  ),
                                  label: Text('点赞 ${item.likeCount}'),
                                ),
                                TextButton.icon(
                                  onPressed: () => _toggleCollect(item),
                                  icon: Icon(
                                    item.collected
                                        ? Icons.bookmark_rounded
                                        : Icons.bookmark_border_rounded,
                                  ),
                                  label: Text('收藏 ${item.collectCount}'),
                                ),
                                TextButton.icon(
                                  onPressed: () => _showComments(item),
                                  icon: const Icon(Icons.mode_comment_outlined),
                                  label: Text('评论 ${item.commentCount}'),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  if (_hasMore)
                    Padding(
                      padding: const EdgeInsets.only(top: 6, bottom: 8),
                      child: FilledButton.tonal(
                        onPressed: _loadingMore ? null : _loadMore,
                        child: Text(_loadingMore ? '加载中...' : '加载更多'),
                      ),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _showComments(Moment item) async {
    final service = AppScope.of(context).momentService;
    final comments = await service.listComments(item.momentId);
    if (!mounted) return;
    final controller = TextEditingController();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: EdgeInsets.fromLTRB(
            16,
            16,
            16,
            MediaQuery.of(sheetContext).viewInsets.bottom + 16,
          ),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('评论', style: TextStyle(fontWeight: FontWeight.w800)),
                const SizedBox(height: 8),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 300),
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: comments.length,
                    separatorBuilder: (context, _) => const Divider(height: 14),
                    itemBuilder: (context, index) {
                      final c = comments[index];
                      return Text('${c.userNickname}: ${c.content}');
                    },
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: controller,
                        decoration: const InputDecoration(hintText: '写评论'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    FilledButton(
                      onPressed: () async {
                        final text = controller.text.trim();
                        if (text.isEmpty) return;
                        try {
                          await service.addComment(
                            momentId: item.momentId,
                            content: text,
                          );
                          if (!sheetContext.mounted) return;
                          Navigator.of(sheetContext).pop();
                          await _reload();
                        } on ApiException catch (e) {
                          messenger.showSnackBar(
                            SnackBar(content: Text(e.message)),
                          );
                        }
                      },
                      child: const Text('发送'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _showComposer() async {
    final service = AppScope.of(context).momentService;
    final fileService = AppScope.of(context).fileService;
    final contentController = TextEditingController();
    final selected = <UploadAsset>[];
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: EdgeInsets.fromLTRB(
            16,
            16,
            16,
            MediaQuery.of(sheetContext).viewInsets.bottom + 16,
          ),
          child: StatefulBuilder(
            builder: (context, setStateSheet) {
              return GlassCard(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text(
                      '发布动态',
                      style: TextStyle(fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: contentController,
                      maxLines: 4,
                      decoration: const InputDecoration(hintText: '说点什么...'),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        OutlinedButton.icon(
                          onPressed: () async {
                            final pick = await FilePicker.platform.pickFiles(
                              type: FileType.image,
                              allowMultiple: true,
                            );
                            final files = pick?.files ?? const [];
                            for (final f in files) {
                              if (f.path == null) continue;
                              final up = await fileService.uploadFile(
                                filePath: f.path!,
                                fileName: f.name,
                                scene: 'moment',
                              );
                              selected.add(up);
                            }
                            setStateSheet(() {});
                          },
                          icon: const Icon(Icons.photo_library_outlined),
                          label: Text(
                            selected.isEmpty
                                ? '添加图片'
                                : '已选 ${selected.length} 张',
                          ),
                        ),
                        const Spacer(),
                        FilledButton(
                          onPressed: () async {
                            final content = contentController.text.trim();
                            if (content.isEmpty && selected.isEmpty) return;
                            try {
                              await service.createMoment(
                                content: content,
                                fileIds: selected.map((e) => e.fileId).toList(),
                              );
                              if (!sheetContext.mounted) return;
                              Navigator.of(sheetContext).pop();
                              await _reload();
                            } on ApiException catch (e) {
                              messenger.showSnackBar(
                                SnackBar(content: Text(e.message)),
                              );
                            }
                          },
                          child: const Text('发布'),
                        ),
                      ],
                    ),
                  ],
                ),
              );
            },
          ),
        );
      },
    );
  }

  Future<void> _showNotifications() async {
    final service = AppScope.of(context).momentService;
    final items = <MomentNotification>[];
    var page = 1;
    var hasMore = true;
    var loading = true;
    var loadingMore = false;

    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return StatefulBuilder(
          builder: (context, setSheetState) {
            Future<void> loadPage({required bool append}) async {
              if (append) {
                if (!hasMore || loadingMore) return;
                setSheetState(() => loadingMore = true);
              } else {
                setSheetState(() => loading = true);
              }
              try {
                final paged = await service.listNotifications(
                  page: append ? page + 1 : 1,
                  pageSize: 20,
                );
                setSheetState(() {
                  if (append) {
                    items.addAll(paged.list);
                  } else {
                    items
                      ..clear()
                      ..addAll(paged.list);
                  }
                  page = paged.page;
                  hasMore = paged.hasMore;
                });
              } on ApiException catch (e) {
                messenger.showSnackBar(SnackBar(content: Text(e.message)));
              } finally {
                setSheetState(() {
                  if (append) {
                    loadingMore = false;
                  } else {
                    loading = false;
                  }
                });
              }
            }

            if (loading && items.isEmpty) {
              WidgetsBinding.instance.addPostFrameCallback(
                (_) => loadPage(append: false),
              );
            }

            return Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
              child: GlassCard(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Expanded(
                          child: Text(
                            '互动通知',
                            style: TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        TextButton(
                          onPressed: () async {
                            await service.markAllNotificationsRead();
                            await _loadUnreadCount();
                            await loadPage(append: false);
                          },
                          child: const Text('全部已读'),
                        ),
                      ],
                    ),
                    if (loading && items.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 24),
                        child: Center(child: CircularProgressIndicator()),
                      )
                    else if (items.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12),
                        child: Text('当前没有新的互动通知。'),
                      )
                    else
                      ConstrainedBox(
                        constraints: const BoxConstraints(maxHeight: 420),
                        child: ListView.separated(
                          shrinkWrap: true,
                          itemCount: items.length + 1,
                          separatorBuilder: (context, _) =>
                              const Divider(height: 16),
                          itemBuilder: (context, index) {
                            if (index == items.length) {
                              if (loadingMore) {
                                return const Center(
                                  child: CircularProgressIndicator(),
                                );
                              }
                              if (!hasMore) {
                                return const Center(
                                  child: Padding(
                                    padding: EdgeInsets.all(8),
                                    child: Text('没有更多了'),
                                  ),
                                );
                              }
                              return Center(
                                child: TextButton(
                                  onPressed: () => loadPage(append: true),
                                  child: const Text('加载更多'),
                                ),
                              );
                            }
                            final item = items[index];
                            return ListTile(
                              title: Text(
                                item.title.isEmpty ? '互动提醒' : item.title,
                              ),
                              subtitle: Text(item.content),
                              trailing: item.read
                                  ? null
                                  : const Icon(
                                      Icons.fiber_new_rounded,
                                      color: AppTheme.primaryBlue,
                                    ),
                              onTap: () async {
                                if (item.read) return;
                                await service.markNotificationRead(
                                  item.notificationId,
                                );
                                await _loadUnreadCount();
                                await loadPage(append: false);
                              },
                            );
                          },
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _showCollections() async {
    final service = AppScope.of(context).momentService;
    final items = <Moment>[];
    var page = 1;
    var hasMore = true;
    var loading = true;
    var loadingMore = false;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return StatefulBuilder(
          builder: (context, setSheetState) {
            Future<void> loadPage({required bool append}) async {
              if (append) {
                if (!hasMore || loadingMore) return;
                setSheetState(() => loadingMore = true);
              } else {
                setSheetState(() => loading = true);
              }
              try {
                final paged = await service.listCollectedMoments(
                  page: append ? page + 1 : 1,
                  pageSize: 20,
                );
                setSheetState(() {
                  if (append) {
                    items.addAll(paged.list);
                  } else {
                    items
                      ..clear()
                      ..addAll(paged.list);
                  }
                  page = paged.page;
                  hasMore = paged.hasMore;
                });
              } on ApiException catch (e) {
                messenger.showSnackBar(SnackBar(content: Text(e.message)));
              } finally {
                setSheetState(() {
                  if (append) {
                    loadingMore = false;
                  } else {
                    loading = false;
                  }
                });
              }
            }

            if (loading && items.isEmpty) {
              WidgetsBinding.instance.addPostFrameCallback(
                (_) => loadPage(append: false),
              );
            }

            return Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
              child: GlassCard(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Expanded(
                          child: Text(
                            '我的收藏',
                            style: TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        IconButton(
                          onPressed: loading || loadingMore
                              ? null
                              : () => loadPage(append: false),
                          icon: const Icon(Icons.refresh_rounded),
                        ),
                      ],
                    ),
                    if (loading && items.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 24),
                        child: Center(child: CircularProgressIndicator()),
                      )
                    else if (items.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 12),
                        child: Text('当前没有收藏动态。'),
                      )
                    else
                      ConstrainedBox(
                        constraints: const BoxConstraints(maxHeight: 420),
                        child: ListView.separated(
                          shrinkWrap: true,
                          itemCount: items.length + 1,
                          separatorBuilder: (context, _) =>
                              const Divider(height: 16),
                          itemBuilder: (context, index) {
                            if (index == items.length) {
                              if (loadingMore) {
                                return const Center(
                                  child: CircularProgressIndicator(),
                                );
                              }
                              if (!hasMore) {
                                return const Center(
                                  child: Padding(
                                    padding: EdgeInsets.all(8),
                                    child: Text('没有更多了'),
                                  ),
                                );
                              }
                              return Center(
                                child: TextButton(
                                  onPressed: () => loadPage(append: true),
                                  child: const Text('加载更多'),
                                ),
                              );
                            }
                            final item = items[index];
                            return ListTile(
                              title: Text(item.authorNickname),
                              subtitle: Text(
                                item.content.trim().isEmpty
                                    ? '[图片动态]'
                                    : item.content,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              onTap: () => _showComments(item),
                            );
                          },
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  String _formatTime(String value) {
    return value.replaceFirst('T', ' ').split('.').first;
  }
}
