import 'package:app/app/app_scope.dart';
import 'package:app/app/shell/app_shell.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/features/auth/presentation/auth_page.dart';
import 'package:app/core/storage/session_store.dart';
import 'package:flutter/material.dart';

class HelloChatApp extends StatefulWidget {
  const HelloChatApp({super.key});

  @override
  State<HelloChatApp> createState() => _HelloChatAppState();
}

class _HelloChatAppState extends State<HelloChatApp> {
  final SessionStore _sessionStore = SessionStore();
  bool _loaded = false;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    await _sessionStore.load();
    if (mounted) {
      setState(() {
        _loaded = true;
      });
    }
  }

  void _handleLogin() {
    setState(() {});
  }

  void _handleLogout() {
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return AppScope(
      sessionStore: _sessionStore,
      child: MaterialApp(
        title: 'Hello Chat Mobile',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light(),
        home: !_loaded
            ? const Scaffold(body: Center(child: CircularProgressIndicator()))
            : AnimatedBuilder(
                animation: _sessionStore,
                builder: (context, _) {
                  return AnimatedSwitcher(
                    duration: const Duration(milliseconds: 360),
                    switchInCurve: Curves.easeOutCubic,
                    switchOutCurve: Curves.easeInCubic,
                    child: _sessionStore.session != null
                        ? AppShell(
                            key: const ValueKey('shell'),
                            onLogout: _handleLogout,
                          )
                        : AuthPage(
                            key: const ValueKey('auth'),
                            onLogin: _handleLogin,
                          ),
                  );
                },
              ),
      ),
    );
  }
}
