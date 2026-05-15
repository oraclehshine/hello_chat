import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/chat_summary.dart';
import 'package:app/features/chat/presentation/chat_detail_page.dart';
import 'package:app/features/chat/presentation/messages_page.dart';
import 'package:app/features/friends/presentation/friends_page.dart';
import 'package:app/features/groups/presentation/groups_page.dart';
import 'package:app/features/moments/presentation/moments_page.dart';
import 'package:app/features/profile/presentation/profile_page.dart';
import 'package:app/shared/widgets/brand_mark.dart';
import 'package:flutter/material.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key, required this.onLogout});

  final VoidCallback onLogout;

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _currentIndex = 0;
  bool _initialized = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_initialized) return;
    _initialized = true;
    final scope = AppScope.of(context);
    scope.chatSocketService.ensureConnected();
    scope.notificationService.initialize();
  }

  void _openChat(ChatSummary chat) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => ChatDetailPage(chat: chat)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      MessagesPage(onOpenChat: _openChat),
      FriendsPage(onOpenChat: _openChat),
      const GroupsPage(),
      const MomentsPage(),
      ProfilePage(onLogout: widget.onLogout),
    ];

    return Scaffold(
      extendBody: true,
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFFF6FAFF), Color(0xFFE7F0FF), Color(0xFFF8FBFF)],
          ),
        ),
        child: SafeArea(
          child: Stack(
            children: [
              Positioned(
                left: -80,
                top: -30,
                child: _GlowOrb(
                  size: 220,
                  color: AppTheme.primaryBlue.withValues(alpha: 0.10),
                ),
              ),
              Positioned(
                right: -40,
                top: 180,
                child: _GlowOrb(
                  size: 140,
                  color: const Color(0xFF8AB7FF).withValues(alpha: 0.18),
                ),
              ),
              Positioned.fill(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 280),
                  child: KeyedSubtree(
                    key: ValueKey<int>(_currentIndex),
                    child: pages[_currentIndex],
                  ),
                ),
              ),
              const Positioned(
                top: 18,
                left: 20,
                child: IgnorePointer(
                  child: Row(
                    children: [
                      BrandMark(size: 32),
                      SizedBox(width: 12),
                      Text(
                        'Hello Chat',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.80),
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: AppTheme.border),
            boxShadow: [
              BoxShadow(
                color: AppTheme.primaryBlue.withValues(alpha: 0.10),
                blurRadius: 28,
                offset: const Offset(0, 12),
              ),
            ],
          ),
          child: NavigationBar(
            selectedIndex: _currentIndex,
            height: 62,
            backgroundColor: Colors.transparent,
            surfaceTintColor: Colors.transparent,
            indicatorColor: AppTheme.primaryBlue.withValues(alpha: 0.16),
            labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
            destinations: const [
              NavigationDestination(
                icon: Icon(Icons.chat_bubble_outline_rounded, size: 21),
                selectedIcon: Icon(Icons.chat_bubble_rounded, size: 21),
                label: '消息',
              ),
              NavigationDestination(
                icon: Icon(Icons.people_outline_rounded, size: 21),
                selectedIcon: Icon(Icons.people_rounded, size: 21),
                label: '好友',
              ),
              NavigationDestination(
                icon: Icon(Icons.forum_outlined, size: 21),
                selectedIcon: Icon(Icons.forum_rounded, size: 21),
                label: '群聊',
              ),
              NavigationDestination(
                icon: Icon(Icons.auto_awesome_mosaic_outlined, size: 21),
                selectedIcon: Icon(Icons.auto_awesome_mosaic_rounded, size: 21),
                label: '朋友圈',
              ),
              NavigationDestination(
                icon: Icon(Icons.person_outline_rounded, size: 21),
                selectedIcon: Icon(Icons.person_rounded, size: 21),
                label: '我的',
              ),
            ],
            onDestinationSelected: (index) => setState(() => _currentIndex = index),
          ),
        ),
      ),
    );
  }
}

class _GlowOrb extends StatelessWidget {
  const _GlowOrb({required this.size, required this.color});

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(colors: [color, color.withValues(alpha: 0)]),
      ),
    );
  }
}
