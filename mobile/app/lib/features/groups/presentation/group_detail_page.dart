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
  StreamSubscription<ChatSocketEvent>? _socketSubscription;

  late GroupSummary _group;
  bool _loading = true;
  bool _sending = false;

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
            onPressed: _reload,
            icon: const Icon(Icons.refresh_rounded),
          ),
          PopupMenuButton<String>(
            onSelected: (value) async {
              if (value == 'leave') {
                await _confirmLeaveGroup();
              } else if (value == 'dissolve') {
                await _confirmDissolveGroup();
              }
            },
            itemBuilder: (context) => const [
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
                      return Column(
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
                          Container(
                            constraints: const BoxConstraints(maxWidth: 280),
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
}
