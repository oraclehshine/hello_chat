import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/shared/widgets/brand_mark.dart';
import 'package:flutter/material.dart';

enum AuthMode { login, register, reset }

class AuthPage extends StatefulWidget {
  const AuthPage({super.key, required this.onLogin});

  final VoidCallback onLogin;

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  AuthMode _mode = AuthMode.login;
  bool _submitting = false;
  bool _sendingCaptcha = false;

  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _captchaController = TextEditingController();
  final _nicknameController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _captchaController.dispose();
    _nicknameController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final headline = switch (_mode) {
      AuthMode.login => '登录',
      AuthMode.register => '注册',
      AuthMode.reset => '找回密码',
    };

    final buttonText = switch (_mode) {
      AuthMode.login => '登录',
      AuthMode.register => '注册',
      AuthMode.reset => '更新密码',
    };

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF2D7DFF), Color(0xFF7BB2FF), Color(0xFFF5F9FF)],
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(22, 18, 22, 18),
            child: Column(
              children: [
                Row(
                  children: [
                    const BrandMark(),
                    const SizedBox(width: 12),
                    Text(
                      'Hello Chat',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Expanded(
                  child: Center(
                    child: Container(
                      width: double.infinity,
                      constraints: const BoxConstraints(maxWidth: 440),
                      padding: const EdgeInsets.fromLTRB(22, 24, 22, 22),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.86),
                        borderRadius: BorderRadius.circular(32),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.5),
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.08),
                            blurRadius: 34,
                            offset: const Offset(0, 18),
                          ),
                        ],
                      ),
                      child: SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              headline,
                              style: Theme.of(context).textTheme.headlineMedium,
                            ),
                            const SizedBox(height: 20),
                            SegmentedButton<AuthMode>(
                              segments: const [
                                ButtonSegment<AuthMode>(
                                  value: AuthMode.login,
                                  icon: Icon(Icons.login_rounded),
                                  label: Text('登录'),
                                ),
                                ButtonSegment<AuthMode>(
                                  value: AuthMode.register,
                                  icon: Icon(Icons.person_add_alt_1_rounded),
                                  label: Text('注册'),
                                ),
                              ],
                              selected: {
                                _mode == AuthMode.reset
                                    ? AuthMode.login
                                    : _mode,
                              },
                              onSelectionChanged: (value) {
                                setState(() {
                                  _mode = value.first;
                                });
                              },
                            ),
                            const SizedBox(height: 22),
                            const _Label('邮箱'),
                            const SizedBox(height: 8),
                            TextField(
                              controller: _emailController,
                              decoration: const InputDecoration(
                                hintText: '输入邮箱',
                                prefixIcon: Icon(Icons.mail_outline_rounded),
                              ),
                            ),
                            if (_mode != AuthMode.login) ...[
                              const SizedBox(height: 14),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextField(
                                      controller: _captchaController,
                                      decoration: const InputDecoration(
                                        hintText: '输入邮箱验证码',
                                        prefixIcon: Icon(
                                          Icons.mark_email_read_outlined,
                                        ),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  SizedBox(
                                    height: 56,
                                    child: OutlinedButton(
                                      onPressed: _sendingCaptcha
                                          ? null
                                          : _sendCaptcha,
                                      style: OutlinedButton.styleFrom(
                                        foregroundColor: AppTheme.primaryBlue,
                                        side: const BorderSide(
                                          color: AppTheme.border,
                                        ),
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(
                                            18,
                                          ),
                                        ),
                                      ),
                                      child: Text(
                                        _sendingCaptcha ? '发送中' : '获取验证码',
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                            if (_mode == AuthMode.register) ...[
                              const SizedBox(height: 14),
                              const _Label('昵称'),
                              const SizedBox(height: 8),
                              TextField(
                                controller: _nicknameController,
                                decoration: const InputDecoration(
                                  hintText: '输入昵称',
                                  prefixIcon: Icon(Icons.badge_outlined),
                                ),
                              ),
                            ],
                            const SizedBox(height: 14),
                            const _Label('密码'),
                            const SizedBox(height: 8),
                            TextField(
                              controller: _passwordController,
                              obscureText: true,
                              decoration: const InputDecoration(
                                hintText: '输入密码',
                                prefixIcon: Icon(Icons.lock_outline_rounded),
                              ),
                            ),
                            if (_mode == AuthMode.register) ...[
                              const SizedBox(height: 14),
                              const _Label('确认密码'),
                              const SizedBox(height: 8),
                              TextField(
                                controller: _confirmPasswordController,
                                obscureText: true,
                                decoration: const InputDecoration(
                                  hintText: '再次输入密码',
                                  prefixIcon: Icon(
                                    Icons.verified_user_outlined,
                                  ),
                                ),
                              ),
                            ],
                            const SizedBox(height: 22),
                            ElevatedButton(
                              onPressed: _submitting ? null : _submit,
                              child: Text(_submitting ? '处理中...' : buttonText),
                            ),
                            if (_mode == AuthMode.login) ...[
                              const SizedBox(height: 10),
                              Align(
                                alignment: Alignment.centerRight,
                                child: TextButton(
                                  onPressed: () {
                                    setState(() {
                                      _mode = AuthMode.reset;
                                    });
                                  },
                                  style: TextButton.styleFrom(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 4,
                                      vertical: 0,
                                    ),
                                    minimumSize: const Size(0, 28),
                                    tapTargetSize:
                                        MaterialTapTargetSize.shrinkWrap,
                                  ),
                                  child: const Text(
                                    '找回密码',
                                    style: TextStyle(fontSize: 12),
                                  ),
                                ),
                              ),
                            ] else if (_mode == AuthMode.reset) ...[
                              const SizedBox(height: 10),
                              Align(
                                alignment: Alignment.centerRight,
                                child: TextButton(
                                  onPressed: () {
                                    setState(() {
                                      _mode = AuthMode.login;
                                    });
                                  },
                                  style: TextButton.styleFrom(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 4,
                                      vertical: 0,
                                    ),
                                    minimumSize: const Size(0, 28),
                                    tapTargetSize:
                                        MaterialTapTargetSize.shrinkWrap,
                                  ),
                                  child: const Text(
                                    '返回登录',
                                    style: TextStyle(fontSize: 12),
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _sendCaptcha() async {
    setState(() {
      _sendingCaptcha = true;
    });
    try {
      final scene = _mode == AuthMode.reset ? 'reset_password' : 'register';
      await AppScope.of(
        context,
      ).authService.sendCaptcha(_emailController.text.trim(), scene);
      _toast('验证码已发送');
    } on ApiException catch (error) {
      _toast(error.message);
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _sendingCaptcha = false;
        });
      }
    }
  }

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
    });

    try {
      final authService = AppScope.of(context).authService;
      switch (_mode) {
        case AuthMode.login:
          await authService.login(
            email: _emailController.text.trim(),
            password: _passwordController.text,
          );
          widget.onLogin();
          break;
        case AuthMode.register:
          if (_passwordController.text != _confirmPasswordController.text) {
            throw const ApiException('两次密码不一致');
          }
          await authService.register(
            email: _emailController.text.trim(),
            password: _passwordController.text,
            captcha: _captchaController.text.trim(),
          );
          if (_nicknameController.text.trim().isNotEmpty) {
            await authService.updateProfile(
              nickname: _nicknameController.text.trim(),
            );
          }
          widget.onLogin();
          break;
        case AuthMode.reset:
          await authService.resetPassword(
            email: _emailController.text.trim(),
            captcha: _captchaController.text.trim(),
            newPassword: _passwordController.text,
          );
          if (mounted) {
            setState(() {
              _mode = AuthMode.login;
            });
          }
          _toast('密码已更新');
          break;
      }
    } on ApiException catch (error) {
      _toast(error.message);
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _submitting = false;
        });
      }
    }
  }

  void _toast(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _Label extends StatelessWidget {
  const _Label(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        fontWeight: FontWeight.w700,
        color: AppTheme.textPrimary,
      ),
    );
  }
}
