import 'dart:async';

import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/chat_message.dart';
import 'package:app/core/models/chat_summary.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/core/realtime/chat_socket_service.dart';
import 'package:file_picker/file_picker.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/attachment_message_body.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:flutter/material.dart';

class ChatDetailPage extends StatefulWidget {
  const ChatDetailPage({super.key, required this.chat});

  final ChatSummary chat;

  @override
  State<ChatDetailPage> createState() => _ChatDetailPageState();
}

class _ChatDetailPageState extends State<ChatDetailPage> {
  final _inputController = TextEditingController();
  final List<ChatMessage> _messages = <ChatMessage>[];
  final ScrollController _scrollController = ScrollController();
  final Map<int, GlobalKey> _messageKeys = <int, GlobalKey>{};
  StreamSubscription<ChatSocketEvent>? _socketSubscription;
  Timer? _typingTimer;
  bool _loading = true;
  bool _sending = false;
  bool _peerTyping = false;
  bool _typingSent = false;
  int? _highlightMessageId;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _bindRealtime();
    _bootstrap();
  }

  @override
  void dispose() {
    _typingTimer?.cancel();
    _socketSubscription?.cancel();
    _scrollController.dispose();
    _inputController.dispose();
    _sendTyping(false);
    super.dispose();
  }

  Future<void> _bootstrap() async {
    await AppScope.of(context).chatSocketService.ensureConnected();
    await _loadMessages();
  }

  void _bindRealtime() {
    _socketSubscription?.cancel();
    _socketSubscription = AppScope.of(
      context,
    ).chatSocketService.events.listen(_handleSocketEvent);
  }

  Future<void> _loadMessages() async {
    setState(() {
      _loading = true;
    });
    try {
      final service = AppScope.of(context).chatService;
      final result = await service.listMessages(widget.chat.chatId);
      await service.markChatAsRead(widget.chat.chatId);
      if (!mounted) return;
      setState(() {
        _messages
          ..clear()
          ..addAll(result.list.reversed);
        _loading = false;
      });
      _scrollToBottom();
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _reload() async {
    await _loadMessages();
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
      ).chatService.sendMessage(chatId: widget.chat.chatId, content: content);
      _inputController.clear();
      await _sendTyping(false);
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
    final messenger = ScaffoldMessenger.of(context);
    final result = await FilePicker.platform.pickFiles(
      allowMultiple: false,
      type: imagesOnly ? FileType.image : FileType.any,
      withData: false,
    );
    final files = result?.files;
    final picked = files == null || files.isEmpty ? null : files.first;
    final path = picked?.path;
    if (picked == null || path == null) {
      return;
    }

    setState(() {
      _sending = true;
    });
    try {
      final upload = await scope.fileService.uploadFile(
        filePath: path,
        fileName: picked.name,
        scene: 'attachment',
      );
      await scope.chatService.sendAttachment(
        chatId: widget.chat.chatId,
        fileId: upload.fileId,
        fileName: upload.fileName,
        fileSize: upload.fileSize,
        isImage: imagesOnly,
      );
    } on ApiException catch (error) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) {
        setState(() {
          _sending = false;
        });
      }
    }
  }

  void _handleInputChanged(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) {
      _typingTimer?.cancel();
      _sendTyping(false);
      return;
    }

    if (!_typingSent) {
      _sendTyping(true);
    }
    _typingTimer?.cancel();
    _typingTimer = Timer(const Duration(milliseconds: 1400), () {
      _sendTyping(false);
    });
  }

  Future<void> _sendTyping(bool typing) async {
    if (_typingSent == typing) {
      return;
    }
    _typingSent = typing;
    try {
      await AppScope.of(context).chatService.updateTypingStatus(
        chatId: widget.chat.chatId,
        typing: typing,
      );
    } catch (_) {
      // Keep typing indicator best-effort only.
    }
  }

  void _handleSocketEvent(ChatSocketEvent event) {
    if (!mounted) return;
    switch (event.eventType) {
      case 'message:new':
        final message = ChatMessage.fromJson(event.payload);
        if (message.chatId != widget.chat.chatId) {
          return;
        }
        final exists = _messages.any(
          (item) => item.messageId == message.messageId,
        );
        if (!exists) {
          setState(() {
            _messages.add(message);
          });
          _scrollToBottom();
        }
        break;
      case 'message:update':
        final chatId = (event.payload['chatId'] as num?)?.toInt() ?? 0;
        if (chatId == widget.chat.chatId) {
          _loadMessages();
        }
        break;
      case 'typing:update':
        final chatId = (event.payload['chatId'] as num?)?.toInt() ?? 0;
        final userId = (event.payload['userId'] as num?)?.toInt() ?? 0;
        final typing = event.payload['typing'] as bool? ?? false;
        if (chatId == widget.chat.chatId &&
            userId == widget.chat.targetUserId) {
          setState(() {
            _peerTyping = typing;
          });
        }
        break;
      default:
        break;
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

  Future<void> _locateMessageFromSearch(ChatMessage target) async {
    final messenger = ScaffoldMessenger.of(context);
    final targetId = target.messageId;
    var exists = _messages.any((item) => item.messageId == targetId);
    if (!exists) {
      await _loadMessages();
      if (!mounted) return;
      exists = _messages.any((item) => item.messageId == targetId);
      if (!exists) {
        messenger.showSnackBar(const SnackBar(content: Text('该消息不在当前已加载列表中。')));
        return;
      }
    }
    setState(() {
      _highlightMessageId = targetId;
    });
    await Future<void>.delayed(const Duration(milliseconds: 40));
    final key = _messageKeys[targetId];
    final targetContext = key?.currentContext;
    if (targetContext == null) {
      messenger.showSnackBar(const SnackBar(content: Text('定位失败，请下拉刷新后重试。')));
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
              label: widget.chat.targetNickname,
              size: 40,
              imageUrl: widget.chat.targetAvatarUrl,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.chat.targetNickname,
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  if (_peerTyping)
                    const Text(
                      '姝ｅ湪杈撳叆...',
                      style: TextStyle(
                        fontSize: 12,
                        color: AppTheme.primaryBlue,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                ],
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
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _messages.isEmpty
                ? const Center(child: Text('还没有消息，发送第一条消息开始对话。'))
                : ListView.separated(
                    controller: _scrollController,
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
                    itemCount: _messages.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: 14),
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
                                mine ? '我' : widget.chat.targetNickname,
                                style: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                  color: AppTheme.textSecondary,
                                ),
                              ),
                              const SizedBox(height: 6),
                              if (message.pinnedAt?.isNotEmpty == true)
                                const Padding(
                                  padding: EdgeInsets.only(bottom: 6),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Icon(
                                        Icons.push_pin_rounded,
                                        size: 12,
                                        color: AppTheme.primaryBlue,
                                      ),
                                      SizedBox(width: 4),
                                      Text(
                                        '已置顶',
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
                                    boxShadow: [
                                      BoxShadow(
                                        color: AppTheme.primaryBlue.withValues(
                                          alpha: 0.08,
                                        ),
                                        blurRadius: 20,
                                        offset: const Offset(0, 10),
                                      ),
                                    ],
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
                    onChanged: _handleInputChanged,
                    decoration: const InputDecoration(
                      hintText: '杈撳叆娑堟伅鍐呭',
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
                        onPressed: () {},
                        icon: const Icon(Icons.mic_none_rounded),
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
    required ChatMessage message,
    required bool mine,
  }) async {
    final chatService = AppScope.of(context).chatService;
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
                  '娑堟伅鎿嶄綔',
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
                    title: const Text('鎾ゅ洖娑堟伅'),
                    onTap: () async {
                      Navigator.of(sheetContext).pop();
                      try {
                        await chatService.recallMessage(message.messageId);
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
                  leading: Icon(
                    message.pinnedAt?.isNotEmpty == true
                        ? Icons.push_pin_outlined
                        : Icons.push_pin_rounded,
                  ),
                  title: Text(
                    message.pinnedAt?.isNotEmpty == true ? '鍙栨秷缃《' : '缃《娑堟伅',
                  ),
                  onTap: () async {
                    Navigator.of(sheetContext).pop();
                    try {
                      if (message.pinnedAt?.isNotEmpty == true) {
                        await chatService.unpinMessage(message.messageId);
                      } else {
                        await chatService.pinMessage(message.messageId);
                      }
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
                    '鍒犻櫎娑堟伅',
                    style: TextStyle(color: Colors.redAccent),
                  ),
                  onTap: () async {
                    Navigator.of(sheetContext).pop();
                    try {
                      await chatService.deleteMessage(message.messageId);
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
    final results = ValueNotifier<List<ChatMessage>>(<ChatMessage>[]);
    final loading = ValueNotifier<bool>(false);
    final messenger = ScaffoldMessenger.of(context);
    final chatService = AppScope.of(context).chatService;

    Future<void> doSearch() async {
      final keyword = controller.text.trim();
      if (keyword.isEmpty) {
        results.value = <ChatMessage>[];
        return;
      }
      loading.value = true;
      try {
        final paged = await chatService.searchMessages(
          chatId: widget.chat.chatId,
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
                  '鎼滅储鑱婂ぉ璁板綍',
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
                    FilledButton(onPressed: doSearch, child: const Text('鎼滅储')),
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
                    return ValueListenableBuilder<List<ChatMessage>>(
                      valueListenable: results,
                      builder: (context, items, child) {
                        if (items.isEmpty) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 12),
                            child: Text('输入关键词后可搜索本会话消息。'),
                          );
                        }
                        return ConstrainedBox(
                          constraints: const BoxConstraints(maxHeight: 360),
                          child: ListView.separated(
                            shrinkWrap: true,
                            itemCount: items.length,
                            separatorBuilder: (_, __) =>
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
                                        msg.sentAt
                                            .replaceFirst('T', ' ')
                                            .split('.')
                                            .first,
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
}
