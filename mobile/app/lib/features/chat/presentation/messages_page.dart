import 'dart:async';

import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/chat_summary.dart';
import 'package:app/core/realtime/chat_socket_service.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:app/shared/widgets/section_header.dart';
import 'package:flutter/material.dart';

class MessagesPage extends StatefulWidget {
  const MessagesPage({super.key, required this.onOpenChat});

  final ValueChanged<ChatSummary> onOpenChat;

  @override
  State<MessagesPage> createState() => _MessagesPageState();
}

class _MessagesPageState extends State<MessagesPage> {
  late Future<List<ChatSummary>> _future;
  StreamSubscription<ChatSocketEvent>? _socketSubscription;
  Timer? _reloadDebounce;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _future = AppScope.of(context).chatService.listChats();
    _bindRealtime();
  }

  @override
  void dispose() {
    _reloadDebounce?.cancel();
    _socketSubscription?.cancel();
    super.dispose();
  }

  Future<void> _reload() async {
    setState(() {
      _future = AppScope.of(context).chatService.listChats();
    });
  }

  void _bindRealtime() {
    _socketSubscription?.cancel();
    _socketSubscription = AppScope.of(context).chatSocketService.events.listen((
      event,
    ) {
      switch (event.eventType) {
        case 'message:new':
        case 'message:update':
        case 'message:read':
          _queueReload();
          break;
        default:
          break;
      }
    });
  }

  void _queueReload() {
    _reloadDebounce?.cancel();
    _reloadDebounce = Timer(const Duration(milliseconds: 250), () {
      if (mounted) {
        _reload();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 84, 20, 120),
        children: [
          SectionHeader(
            title: '消息',
            action: IconButton(
              onPressed: _reload,
              icon: const Icon(Icons.refresh_rounded),
            ),
          ),
          const SizedBox(height: 18),
          const TextField(
            decoration: InputDecoration(
              hintText: '搜索会话、昵称或消息',
              prefixIcon: Icon(Icons.search_rounded),
            ),
          ),
          const SizedBox(height: 18),
          FutureBuilder<List<ChatSummary>>(
            future: _future,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(
                  child: Padding(
                    padding: EdgeInsets.only(top: 60),
                    child: CircularProgressIndicator(),
                  ),
                );
              }

              if (snapshot.hasError) {
                return GlassCard(
                  child: Column(
                    children: [
                      const Icon(
                        Icons.error_outline_rounded,
                        color: Colors.redAccent,
                        size: 34,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        snapshot.error.toString(),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                );
              }

              final items = snapshot.data ?? const <ChatSummary>[];
              if (items.isEmpty) {
                return const GlassCard(
                  child: Padding(
                    padding: EdgeInsets.symmetric(vertical: 28),
                    child: Center(child: Text('当前还没有会话，可从好友页发起单聊。')),
                  ),
                );
              }

              return Column(
                children: items
                    .map(
                      (item) => Padding(
                        padding: const EdgeInsets.only(bottom: 14),
                        child: InkWell(
                          borderRadius: BorderRadius.circular(24),
                          onTap: () => widget.onOpenChat(item),
                          child: GlassCard(
                            child: Row(
                              children: [
                                AppAvatar(
                                  label: item.targetNickname,
                                  size: 56,
                                  imageUrl: item.targetAvatarUrl,
                                ),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Expanded(
                                            child: Text(
                                              item.targetNickname,
                                              style: const TextStyle(
                                                fontWeight: FontWeight.w800,
                                                color: AppTheme.textPrimary,
                                              ),
                                            ),
                                          ),
                                          Text(
                                            _displayTime(item.lastMessageAt),
                                            style: const TextStyle(
                                              fontSize: 12,
                                              color: AppTheme.textSecondary,
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 8),
                                      Text(
                                        item.lastMessagePreview
                                                    ?.trim()
                                                    .isNotEmpty ==
                                                true
                                            ? item.lastMessagePreview!
                                            : item.targetEmail,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ],
                                  ),
                                ),
                                if (item.unreadCount > 0) ...[
                                  const SizedBox(width: 12),
                                  Container(
                                    width: 24,
                                    height: 24,
                                    alignment: Alignment.center,
                                    decoration: const BoxDecoration(
                                      color: AppTheme.primaryBlue,
                                      shape: BoxShape.circle,
                                    ),
                                    child: Text(
                                      '${item.unreadCount}',
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 11,
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        ),
                      ),
                    )
                    .toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  String _displayTime(String? value) {
    if (value == null || value.isEmpty) return '';
    return value.replaceFirst('T', ' ').split('.').first;
  }
}
