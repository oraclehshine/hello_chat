import 'dart:async';

import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/group.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/core/realtime/chat_socket_service.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/attachment_message_body.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

class GroupDetailPage extends StatefulWidget {
  const GroupDetailPage({super.key, required this.group});

  final GroupSummary group;

  @override
  State<GroupDetailPage> createState() => _GroupDetailPageState();
}

class _GroupDetailPageState extends State<GroupDetailPage> {
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final List<GroupMessage> _messages = <GroupMessage>[];
  final Map<int, GlobalKey> _messageKeys = <int, GlobalKey>{};
  StreamSubscription<ChatSocketEvent>? _socketSubscription;

  late GroupSummary _group;
  bool _loading = true;
  bool _sending = false;
  int? _highlightMessageId;

  @override
  void initState() {
    super.initState();
    _group = widget.group;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _socketSubscription ??= AppScope.of(
      context,
    ).chatSocketService.events.listen(_handleSocketEvent);
    _bootstrap();
  }

  @override
  void dispose() {
    _socketSubscription?.cancel();
    _scrollController.dispose();
    _inputController.dispose();
    super.dispose();
  }

  Future<void> _bootstrap() async {
    await AppScope.of(context).chatSocketService.ensureConnected();
    await _reload();
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
    });
    try {
      final scope = AppScope.of(context);
      final latest = await scope.groupService.getGroup(_group.groupId);
      final messages = await scope.groupService.listMessages(_group.groupId);
      await scope.groupService.markAsRead(_group.groupId);
      if (!mounted) return;
      setState(() {
        _group = latest;
        _messages
          ..clear()
          ..addAll(messages.list.reversed);
      });
      _scrollToBottom();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  void _handleSocketEvent(ChatSocketEvent event) {
    if (!mounted) return;
    if (event.eventType == 'group:message:new') {
      final message = GroupMessage.fromJson(event.payload);
      if (message.groupId != _group.groupId) return;
      final exists = _messages.any(
        (item) => item.messageId == message.messageId,
      );
      if (exists) return;
      setState(() {
        _messages.add(message);
      });
      _scrollToBottom();
      return;
    }
    if (event.eventType == 'group:message:update' ||
        event.eventType == 'group:update') {
      _reload();
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent + 60,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOutCubic,
      );
    });
  }

  Future<void> _send() async {
    final content = _inputController.text.trim();
    if (content.isEmpty) return;
    setState(() {
      _sending = true;
    });
    try {
      await AppScope.of(
        context,
      ).groupService.sendMessage(groupId: _group.groupId, content: content);
      _inputController.clear();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _sending = false;
        });
      }
    }
  }

  Future<void> _sendMentionAll() async {
    final content = _inputController.text.trim();
    if (content.isEmpty) return;
    setState(() {
      _sending = true;
    });
    try {
      await AppScope.of(context).groupService.sendMentionAllMessage(
        groupId: _group.groupId,
        content: content,
      );
      _inputController.clear();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _sending = false;
        });
      }
    }
  }

  Future<void> _pickAndSendAttachment({required bool imagesOnly}) async {
    final scope = AppScope.of(context);
    final result = await FilePicker.platform.pickFiles(
      allowMultiple: false,
      type: imagesOnly ? FileType.image : FileType.any,
      withData: false,
    );
    final files = result?.files;
    final picked = files == null || files.isEmpty ? null : files.first;
    final path = picked?.path;
    if (picked == null || path == null) return;

    setState(() {
      _sending = true;
    });

    try {
      final upload = await scope.fileService.uploadFile(
        filePath: path,
        fileName: picked.name,
        scene: 'attachment',
      );
      await scope.groupService.sendAttachment(
        groupId: _group.groupId,
        fileId: upload.fileId,
        fileName: upload.fileName,
        fileSize: upload.fileSize,
        isImage: imagesOnly,
      );
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _sending = false;
        });
      }
    }
  }

  Future<void> _confirmLeaveGroup() async {
    final scope = AppScope.of(context);
    final ok = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('退出群聊'),
        content: const Text('确认退出该群聊吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('确认退出'),
          ),
        ],
      ),
    );
    if (ok != true) return;

    try {
      await scope.groupService.leaveGroup(_group.groupId);
      if (!mounted) return;
      final messenger = ScaffoldMessenger.of(context);
      final navigator = Navigator.of(context);
      messenger.showSnackBar(const SnackBar(content: Text('已退出群聊')));
      navigator.pop();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _confirmDissolveGroup() async {
    final scope = AppScope.of(context);
    final ok = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('解散群聊'),
        content: const Text('该操作不可撤销，确认解散该群聊吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFF15B5B),
            ),
            child: const Text('确认解散'),
          ),
        ],
      ),
    );
    if (ok != true) return;

    try {
      await scope.groupService.dissolveGroup(_group.groupId);
      if (!mounted) return;
      final messenger = ScaffoldMessenger.of(context);
      final navigator = Navigator.of(context);
      messenger.showSnackBar(const SnackBar(content: Text('群聊已解散')));
      navigator.pop();
    } on ApiException catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _locateMessageFromSearch(GroupMessage target) async {
    final messenger = ScaffoldMessenger.maybeOf(context);
    final targetId = target.messageId;
    var exists = _messages.any((item) => item.messageId == targetId);
    if (!exists) {
      await _reload();
      if (!mounted) return;
      exists = _messages.any((item) => item.messageId == targetId);
      if (!exists) {
        messenger?.showSnackBar(
          const SnackBar(content: Text('该消息不在当前已加载列表中。')),
        );
        return;
      }
    }
    setState(() {
      _highlightMessageId = targetId;
    });
    await Future<void>.delayed(const Duration(milliseconds: 40));
    if (!mounted) return;
    final targetContext = _messageKeys[targetId]?.currentContext;
    if (targetContext == null || !targetContext.mounted) {
      messenger?.showSnackBar(const SnackBar(content: Text('定位失败，请刷新后重试。')));
      return;
    }
    await Scrollable.ensureVisible(
      targetContext,
      duration: const Duration(milliseconds: 260),
      curve: Curves.easeOutCubic,
      alignment: 0.3,
    );
    Future<void>.delayed(const Duration(milliseconds: 1800), () {
      if (!mounted || _highlightMessageId != targetId) return;
      setState(() {
        _highlightMessageId = null;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final currentUserId =
        AppScope.of(context).sessionStore.session?.userId ?? 0;

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: Row(
          children: [
            AppAvatar(
              label: _group.groupName,
              size: 40,
              imageUrl: _group.avatarUrl,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                _group.groupName,
                style: const TextStyle(fontWeight: FontWeight.w800),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            onPressed: _showSearchSheet,
            icon: const Icon(Icons.search_rounded),
          ),
          IconButton(
            onPressed: _reload,
            icon: const Icon(Icons.refresh_rounded),
          ),
          PopupMenuButton<String>(
            onSelected: (value) async {
              if (value == 'tools') {
                await _showGroupToolsSheet();
              } else if (value == 'leave') {
                await _confirmLeaveGroup();
              } else if (value == 'dissolve') {
                await _confirmDissolveGroup();
              }
            },
            itemBuilder: (context) => const [
              PopupMenuItem<String>(value: 'tools', child: Text('群管理')),
              PopupMenuItem<String>(value: 'leave', child: Text('退出群聊')),
              PopupMenuItem<String>(value: 'dissolve', child: Text('解散群聊')),
            ],
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
            child: GlassCard(
              child: Text(
                _group.notice?.trim().isNotEmpty == true
                    ? _group.notice!.trim()
                    : '当前暂无群公告。',
                style: const TextStyle(color: AppTheme.textPrimary),
              ),
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _messages.isEmpty
                ? const Center(child: Text('当前群聊还没有消息。'))
                : ListView.separated(
                    controller: _scrollController,
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
                    itemCount: _messages.length,
                    separatorBuilder: (_, index) => const SizedBox(height: 14),
                    itemBuilder: (context, index) {
                      final message = _messages[index];
                      final mine = message.senderId == currentUserId;
                      final bubbleColor = mine
                          ? AppTheme.primaryBlue
                          : Colors.white.withValues(alpha: 0.86);
                      final isHighlighted =
                          _highlightMessageId == message.messageId;
                      final messageKey = _messageKeys.putIfAbsent(
                        message.messageId,
                        GlobalKey.new,
                      );
                      return KeyedSubtree(
                        key: messageKey,
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 240),
                          curve: Curves.easeOutCubic,
                          decoration: BoxDecoration(
                            color: isHighlighted
                                ? AppTheme.primaryBlue.withValues(alpha: 0.1)
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(16),
                          ),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 6,
                          ),
                          child: Column(
                            crossAxisAlignment: mine
                                ? CrossAxisAlignment.end
                                : CrossAxisAlignment.start,
                            children: [
                              Text(
                                mine ? '我' : message.senderNickname,
                                style: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                  color: AppTheme.textSecondary,
                                ),
                              ),
                              const SizedBox(height: 6),
                              if (message.mentionAll)
                                const Padding(
                                  padding: EdgeInsets.only(bottom: 6),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(
                                        Icons.alternate_email_rounded,
                                        size: 12,
                                        color: AppTheme.primaryBlue,
                                      ),
                                      SizedBox(width: 4),
                                      Text(
                                        '@全体成员',
                                        style: TextStyle(
                                          fontSize: 11,
                                          color: AppTheme.primaryBlue,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              GestureDetector(
                                onLongPress: () => _showMessageActions(
                                  message: message,
                                  mine: mine,
                                ),
                                child: Container(
                                  constraints: const BoxConstraints(
                                    maxWidth: 280,
                                  ),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 16,
                                    vertical: 14,
                                  ),
                                  decoration: BoxDecoration(
                                    color: bubbleColor,
                                    borderRadius: BorderRadius.circular(22),
                                    border: mine
                                        ? null
                                        : Border.all(color: AppTheme.border),
                                  ),
                                  child: AttachmentMessageBody(
                                    messageType: message.messageType,
                                    content: message.content,
                                    fileName: message.fileName,
                                    fileSize: message.fileSize,
                                    mine: mine,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                message.sentAt
                                    .replaceFirst('T', ' ')
                                    .split('.')
                                    .first,
                                style: const TextStyle(
                                  fontSize: 11,
                                  color: AppTheme.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: GlassCard(
              padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
              child: Column(
                children: [
                  TextField(
                    controller: _inputController,
                    maxLines: 3,
                    minLines: 1,
                    decoration: const InputDecoration(
                      hintText: '输入群消息内容',
                      border: InputBorder.none,
                      enabledBorder: InputBorder.none,
                      focusedBorder: InputBorder.none,
                      filled: false,
                      contentPadding: EdgeInsets.zero,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      IconButton(
                        onPressed: _sending
                            ? null
                            : () => _pickAndSendAttachment(imagesOnly: true),
                        icon: const Icon(Icons.add_photo_alternate_outlined),
                      ),
                      IconButton(
                        onPressed: _sending
                            ? null
                            : () => _pickAndSendAttachment(imagesOnly: false),
                        icon: const Icon(Icons.attach_file_rounded),
                      ),
                      IconButton(
                        tooltip: '@全体',
                        onPressed: _sending ? null : _sendMentionAll,
                        icon: const Icon(Icons.alternate_email_rounded),
                      ),
                      const Spacer(),
                      FilledButton.icon(
                        onPressed: _sending ? null : _send,
                        icon: const Icon(Icons.send_rounded),
                        label: Text(_sending ? '发送中' : '发送'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showMessageActions({
    required GroupMessage message,
    required bool mine,
  }) async {
    final service = AppScope.of(context).groupService;
    final messenger = ScaffoldMessenger.of(context);
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '消息操作',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 10),
                if (mine)
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.undo_rounded),
                    title: const Text('撤回消息'),
                    onTap: () async {
                      Navigator.of(sheetContext).pop();
                      try {
                        await service.recallMessage(
                          groupId: _group.groupId,
                          messageId: message.messageId,
                        );
                        await _reload();
                      } on ApiException catch (error) {
                        messenger.showSnackBar(
                          SnackBar(content: Text(error.message)),
                        );
                      }
                    },
                  ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(
                    Icons.delete_outline_rounded,
                    color: Colors.redAccent,
                  ),
                  title: const Text(
                    '删除消息',
                    style: TextStyle(color: Colors.redAccent),
                  ),
                  onTap: () async {
                    Navigator.of(sheetContext).pop();
                    try {
                      await service.deleteMessage(
                        groupId: _group.groupId,
                        messageId: message.messageId,
                      );
                      await _reload();
                    } on ApiException catch (error) {
                      messenger.showSnackBar(
                        SnackBar(content: Text(error.message)),
                      );
                    }
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _showSearchSheet() async {
    final controller = TextEditingController();
    final results = ValueNotifier<List<GroupMessage>>(<GroupMessage>[]);
    final loading = ValueNotifier<bool>(false);
    final messenger = ScaffoldMessenger.of(context);
    final groupService = AppScope.of(context).groupService;

    Future<void> doSearch() async {
      final keyword = controller.text.trim();
      if (keyword.isEmpty) {
        results.value = <GroupMessage>[];
        return;
      }
      loading.value = true;
      try {
        final paged = await groupService.searchMessages(
          groupId: _group.groupId,
          keyword: keyword,
        );
        results.value = paged.list;
      } on ApiException catch (error) {
        messenger.showSnackBar(SnackBar(content: Text(error.message)));
      } finally {
        loading.value = false;
      }
    }

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + 16,
          ),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '搜索群消息',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: controller,
                        textInputAction: TextInputAction.search,
                        onSubmitted: (_) => doSearch(),
                        decoration: const InputDecoration(
                          hintText: '输入关键词',
                          prefixIcon: Icon(Icons.search_rounded),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    FilledButton(onPressed: doSearch, child: const Text('搜索')),
                  ],
                ),
                const SizedBox(height: 10),
                ValueListenableBuilder<bool>(
                  valueListenable: loading,
                  builder: (context, isLoading, child) {
                    if (isLoading) {
                      return const Padding(
                        padding: EdgeInsets.symmetric(vertical: 16),
                        child: Center(child: CircularProgressIndicator()),
                      );
                    }
                    return ValueListenableBuilder<List<GroupMessage>>(
                      valueListenable: results,
                      builder: (context, items, child) {
                        if (items.isEmpty) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 12),
                            child: Text('输入关键词后可搜索本群消息。'),
                          );
                        }
                        return ConstrainedBox(
                          constraints: const BoxConstraints(maxHeight: 360),
                          child: ListView.separated(
                            shrinkWrap: true,
                            itemCount: items.length,
                            separatorBuilder: (context, _) =>
                                const Divider(height: 16),
                            itemBuilder: (context, index) {
                              final msg = items[index];
                              return InkWell(
                                borderRadius: BorderRadius.circular(10),
                                onTap: () async {
                                  Navigator.of(sheetContext).pop();
                                  await _locateMessageFromSearch(msg);
                                },
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 4,
                                  ),
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        '${msg.senderNickname} · ${msg.sentAt.replaceFirst('T', ' ').split('.').first}',
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: AppTheme.textSecondary,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        msg.content.trim().isNotEmpty
                                            ? msg.content
                                            : '[${msg.messageType}]',
                                        style: const TextStyle(
                                          color: AppTheme.textPrimary,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                        );
                      },
                    );
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _showGroupToolsSheet() async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _group.groupName,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                if (_group.inviteCode?.isNotEmpty == true) ...[
                  const SizedBox(height: 4),
                  Text('邀请码：${_group.inviteCode}'),
                ],
                const SizedBox(height: 10),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.campaign_outlined),
                  title: const Text('编辑群公告'),
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    _showNoticeSheet();
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.badge_outlined),
                  title: const Text('修改我的群昵称'),
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    _showNicknameSheet();
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.people_outline_rounded),
                  title: const Text('成员管理'),
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    _showMembersSheet();
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.how_to_reg_outlined),
                  title: const Text('入群申请'),
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    _showJoinRequestsSheet();
                  },
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.notifications_none_rounded),
                  title: const Text('群通知'),
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    _showNotificationsSheet();
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _showNoticeSheet() async {
    final controller = TextEditingController(text: _group.notice ?? '');
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + 16,
          ),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '群公告',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: controller,
                  minLines: 3,
                  maxLines: 6,
                  decoration: const InputDecoration(hintText: '填写群公告'),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    const Spacer(),
                    FilledButton(
                      onPressed: () async {
                        try {
                          final updated = await AppScope.of(context)
                              .groupService
                              .updateNotice(
                                groupId: _group.groupId,
                                notice: controller.text.trim(),
                              );
                          if (!sheetContext.mounted) return;
                          Navigator.of(sheetContext).pop();
                          if (!mounted) return;
                          setState(() => _group = updated);
                        } on ApiException catch (error) {
                          messenger.showSnackBar(
                            SnackBar(content: Text(error.message)),
                          );
                        }
                      },
                      child: const Text('保存'),
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

  Future<void> _showNicknameSheet() async {
    final controller = TextEditingController();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + 16,
          ),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '我的群昵称',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: controller,
                  decoration: const InputDecoration(hintText: '新的群昵称'),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    const Spacer(),
                    FilledButton(
                      onPressed: () async {
                        final nickname = controller.text.trim();
                        if (nickname.isEmpty) return;
                        try {
                          await AppScope.of(
                            context,
                          ).groupService.updateMyNickname(
                            groupId: _group.groupId,
                            nickname: nickname,
                          );
                          if (!sheetContext.mounted) return;
                          Navigator.of(sheetContext).pop();
                          await _reload();
                        } on ApiException catch (error) {
                          messenger.showSnackBar(
                            SnackBar(content: Text(error.message)),
                          );
                        }
                      },
                      child: const Text('保存'),
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

  Future<void> _showMembersSheet() async {
    final service = AppScope.of(context).groupService;
    final messenger = ScaffoldMessenger.of(context);
    final members = await service.listMembers(_group.groupId);
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '成员管理',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 10),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 420),
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: members.length,
                    separatorBuilder: (context, _) => const Divider(height: 14),
                    itemBuilder: (context, index) {
                      final member = members[index];
                      return ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: AppAvatar(
                          label: member.nickname,
                          imageUrl: member.avatarUrl,
                          size: 42,
                        ),
                        title: Text(
                          member.groupNickname?.isNotEmpty == true
                              ? member.groupNickname!
                              : member.nickname,
                        ),
                        subtitle: Text(_memberRoleText(member.role)),
                        trailing: PopupMenuButton<String>(
                          onSelected: (value) async {
                            try {
                              if (value == 'admin') {
                                await service.setAdmin(
                                  groupId: _group.groupId,
                                  userId: member.userId,
                                  isAdmin: member.role != 1,
                                );
                              } else if (value == 'mute') {
                                await service.muteMember(
                                  groupId: _group.groupId,
                                  userId: member.userId,
                                  minutes: 10,
                                );
                              } else if (value == 'unmute') {
                                await service.unmuteMember(
                                  groupId: _group.groupId,
                                  userId: member.userId,
                                );
                              } else if (value == 'remove') {
                                await service.removeMember(
                                  groupId: _group.groupId,
                                  userId: member.userId,
                                );
                              }
                              if (!sheetContext.mounted) return;
                              Navigator.of(sheetContext).pop();
                              await _reload();
                            } on ApiException catch (error) {
                              messenger.showSnackBar(
                                SnackBar(content: Text(error.message)),
                              );
                            }
                          },
                          itemBuilder: (context) => [
                            PopupMenuItem<String>(
                              value: 'admin',
                              child: Text(member.role == 1 ? '取消管理员' : '设为管理员'),
                            ),
                            const PopupMenuItem<String>(
                              value: 'mute',
                              child: Text('禁言 10 分钟'),
                            ),
                            const PopupMenuItem<String>(
                              value: 'unmute',
                              child: Text('解除禁言'),
                            ),
                            const PopupMenuItem<String>(
                              value: 'remove',
                              child: Text('移出群聊'),
                            ),
                          ],
                        ),
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
  }

  Future<void> _showJoinRequestsSheet() async {
    final service = AppScope.of(context).groupService;
    final requests = await service.listJoinRequests(_group.groupId);
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '入群申请',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 10),
                if (requests.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: Text('当前没有待处理申请。'),
                  )
                else
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 360),
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: requests.length,
                      separatorBuilder: (context, _) =>
                          const Divider(height: 14),
                      itemBuilder: (context, index) {
                        final request = requests[index];
                        return Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              request.requesterNickname,
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                color: AppTheme.textPrimary,
                              ),
                            ),
                            if (request.message?.isNotEmpty == true)
                              Padding(
                                padding: const EdgeInsets.only(top: 4),
                                child: Text(request.message!),
                              ),
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Expanded(
                                  child: OutlinedButton(
                                    onPressed: () async {
                                      try {
                                        await service.rejectJoinRequest(
                                          groupId: _group.groupId,
                                          requestId: request.requestId,
                                        );
                                        if (!sheetContext.mounted) return;
                                        Navigator.of(sheetContext).pop();
                                        await _reload();
                                      } on ApiException catch (error) {
                                        messenger.showSnackBar(
                                          SnackBar(
                                            content: Text(error.message),
                                          ),
                                        );
                                      }
                                    },
                                    child: const Text('拒绝'),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: FilledButton(
                                    onPressed: () async {
                                      try {
                                        await service.approveJoinRequest(
                                          groupId: _group.groupId,
                                          requestId: request.requestId,
                                        );
                                        if (!sheetContext.mounted) return;
                                        Navigator.of(sheetContext).pop();
                                        await _reload();
                                      } on ApiException catch (error) {
                                        messenger.showSnackBar(
                                          SnackBar(
                                            content: Text(error.message),
                                          ),
                                        );
                                      }
                                    },
                                    child: const Text('通过'),
                                  ),
                                ),
                              ],
                            ),
                          ],
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
  }

  Future<void> _showNotificationsSheet() async {
    final paged = await AppScope.of(
      context,
    ).groupService.listNotifications(groupId: _group.groupId);
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '群通知',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 10),
                if (paged.list.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: Text('当前没有群通知。'),
                  )
                else
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 360),
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: paged.list.length,
                      separatorBuilder: (context, _) =>
                          const Divider(height: 14),
                      itemBuilder: (context, index) {
                        final item = paged.list[index];
                        return ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Text(item.content),
                          subtitle: Text(
                            item.createdAt
                                .replaceFirst('T', ' ')
                                .split('.')
                                .first,
                          ),
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
  }

  String _memberRoleText(int role) {
    switch (role) {
      case 2:
        return '群主';
      case 1:
        return '管理员';
      default:
        return '成员';
    }
  }
}
