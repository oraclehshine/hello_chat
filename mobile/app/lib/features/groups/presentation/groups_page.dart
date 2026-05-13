import 'dart:async';

import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/friend.dart';
import 'package:app/core/models/group.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/core/realtime/chat_socket_service.dart';
import 'package:app/features/groups/presentation/group_detail_page.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:app/shared/widgets/section_header.dart';
import 'package:flutter/material.dart';

class GroupsPage extends StatefulWidget {
  const GroupsPage({super.key});

  @override
  State<GroupsPage> createState() => _GroupsPageState();
}

class _GroupsPageState extends State<GroupsPage> {
  final TextEditingController _searchController = TextEditingController();
  late Future<List<GroupSummary>> _future;
  List<GroupSummary> _searchResults = const <GroupSummary>[];
  bool _searching = false;
  StreamSubscription<ChatSocketEvent>? _socketSubscription;
  Timer? _reloadDebounce;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _future = AppScope.of(context).groupService.listGroups();
    _bindRealtime();
  }

  @override
  void dispose() {
    _reloadDebounce?.cancel();
    _socketSubscription?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  void _bindRealtime() {
    _socketSubscription?.cancel();
    _socketSubscription = AppScope.of(context).chatSocketService.events.listen((
      event,
    ) {
      switch (event.eventType) {
        case 'group:message:new':
        case 'group:message:update':
        case 'group:update':
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

  Future<void> _reload() async {
    setState(() {
      _future = AppScope.of(context).groupService.listGroups();
    });
  }

  Future<void> _searchGroups() async {
    final keyword = _searchController.text.trim();
    if (keyword.isEmpty) {
      setState(() {
        _searchResults = const <GroupSummary>[];
      });
      return;
    }
    setState(() {
      _searching = true;
    });
    try {
      final results = await AppScope.of(
        context,
      ).groupService.searchGroups(keyword);
      if (!mounted) return;
      setState(() {
        _searchResults = results;
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

  Future<void> _createGroup() async {
    final nameController = TextEditingController();
    final descController = TextEditingController();
    final friends = await AppScope.of(context).friendService.listFriends();
    if (!mounted) return;
    final selectedMemberIds = <int>{};

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final navigator = Navigator.of(sheetContext);
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + 16,
            top: 16,
          ),
          child: StatefulBuilder(
            builder: (context, setSheetState) {
              return GlassCard(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Create Group',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: nameController,
                      decoration: const InputDecoration(hintText: 'Group name'),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: descController,
                      minLines: 2,
                      maxLines: 4,
                      decoration: const InputDecoration(
                        hintText: 'Description (optional)',
                      ),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Select members (at least 1 friend)',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    if (friends.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 8),
                        child: Text('No friends yet. Add a friend first.'),
                      )
                    else
                      ConstrainedBox(
                        constraints: const BoxConstraints(maxHeight: 220),
                        child: ListView.separated(
                          shrinkWrap: true,
                          itemCount: friends.length,
                          separatorBuilder: (_, index) =>
                              const Divider(height: 10),
                          itemBuilder: (context, index) {
                            final item = friends[index];
                            return CheckboxListTile(
                              dense: true,
                              contentPadding: EdgeInsets.zero,
                              value: selectedMemberIds.contains(item.userId),
                              onChanged: (checked) {
                                setSheetState(() {
                                  if (checked == true) {
                                    selectedMemberIds.add(item.userId);
                                  } else {
                                    selectedMemberIds.remove(item.userId);
                                  }
                                });
                              },
                              title: Text(_friendLabel(item)),
                              subtitle: item.email.isEmpty
                                  ? null
                                  : Text(item.email),
                            );
                          },
                        ),
                      ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        const Spacer(),
                        TextButton(
                          onPressed: () => navigator.pop(),
                          child: const Text('Cancel'),
                        ),
                        const SizedBox(width: 8),
                        FilledButton(
                          onPressed: () async {
                            final scope = AppScope.of(this.context);
                            final rootMessenger = ScaffoldMessenger.of(
                              this.context,
                            );
                            final groupName = nameController.text.trim();
                            if (groupName.isEmpty) {
                              messenger.showSnackBar(
                                const SnackBar(
                                  content: Text('Please enter group name'),
                                ),
                              );
                              return;
                            }
                            if (selectedMemberIds.isEmpty) {
                              messenger.showSnackBar(
                                const SnackBar(
                                  content: Text('Select at least one friend'),
                                ),
                              );
                              return;
                            }
                            try {
                              await scope.groupService.createGroup(
                                groupName: groupName,
                                description: descController.text,
                                memberIds: selectedMemberIds.toList(),
                              );
                              navigator.pop();
                              if (!mounted) return;
                              rootMessenger.showSnackBar(
                                const SnackBar(content: Text('Group created')),
                              );
                              await _reload();
                            } on ApiException catch (error) {
                              messenger.showSnackBar(
                                SnackBar(content: Text(error.message)),
                              );
                            }
                          },
                          child: const Text('Create'),
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

  Future<void> _joinByInviteCode() async {
    final inviteController = TextEditingController();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) {
        final navigator = Navigator.of(sheetContext);
        final messenger = ScaffoldMessenger.of(sheetContext);
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + 16,
            top: 16,
          ),
          child: GlassCard(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Join by Invite Code',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: inviteController,
                  decoration: const InputDecoration(hintText: 'Invite code'),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    const Spacer(),
                    TextButton(
                      onPressed: () => navigator.pop(),
                      child: const Text('Cancel'),
                    ),
                    const SizedBox(width: 8),
                    FilledButton(
                      onPressed: () async {
                        final inviteCode = inviteController.text.trim();
                        if (inviteCode.isEmpty) return;
                        try {
                          await AppScope.of(
                            context,
                          ).groupService.joinByInviteCode(inviteCode);
                          navigator.pop();
                          if (!mounted) return;
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Joined group')),
                          );
                          await _reload();
                        } on ApiException catch (error) {
                          messenger.showSnackBar(
                            SnackBar(content: Text(error.message)),
                          );
                        }
                      },
                      child: const Text('Join'),
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

  String _friendLabel(FriendItem item) {
    final remark = item.remarkName?.trim();
    if (remark != null && remark.isNotEmpty) return remark;
    if (item.nickname.trim().isNotEmpty) return item.nickname.trim();
    return 'User ${item.userId}';
  }

  Future<void> _openGroup(GroupSummary group) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => GroupDetailPage(group: group)),
    );
    await _reload();
  }

  Widget _groupTile(GroupSummary item) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: AppAvatar(
        label: item.groupName,
        imageUrl: item.avatarUrl,
        size: 42,
      ),
      title: Text(item.groupName),
      subtitle: Text(
        item.description?.trim().isNotEmpty == true
            ? item.description!.trim()
            : 'No description',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: const Icon(Icons.chevron_right_rounded),
      onTap: () => _openGroup(item),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: RefreshIndicator(
        onRefresh: _reload,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 84, 20, 120),
          children: [
            SectionHeader(
              title: 'Groups',
              action: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextButton(
                    onPressed: _createGroup,
                    child: const Text('Create'),
                  ),
                  const SizedBox(width: 6),
                  TextButton(
                    onPressed: _joinByInviteCode,
                    child: const Text('Join'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            GlassCard(
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _searchController,
                      decoration: const InputDecoration(
                        hintText: 'Search public groups',
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: _searching ? null : _searchGroups,
                    icon: _searching
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.search_rounded),
                  ),
                ],
              ),
            ),
            if (_searchResults.isNotEmpty) ...[
              const SizedBox(height: 14),
              const Text(
                'Discover',
                style: TextStyle(
                  fontWeight: FontWeight.w800,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              GlassCard(
                child: Column(
                  children: _searchResults
                      .map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: _groupTile(item),
                        ),
                      )
                      .toList(),
                ),
              ),
            ],
            const SizedBox(height: 14),
            const Text(
              'My Groups',
              style: TextStyle(
                fontWeight: FontWeight.w800,
                color: AppTheme.textPrimary,
              ),
            ),
            const SizedBox(height: 8),
            FutureBuilder<List<GroupSummary>>(
              future: _future,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const GlassCard(
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                if (snapshot.hasError) {
                  final message = snapshot.error is ApiException
                      ? (snapshot.error as ApiException).message
                      : 'Failed to load groups';
                  return GlassCard(child: Text(message));
                }
                final items = snapshot.data ?? const <GroupSummary>[];
                if (items.isEmpty) {
                  return const GlassCard(child: Text('No joined groups yet.'));
                }
                return GlassCard(
                  child: Column(
                    children: items
                        .map(
                          (item) => Padding(
                            padding: const EdgeInsets.only(bottom: 8),
                            child: _groupTile(item),
                          ),
                        )
                        .toList(),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
