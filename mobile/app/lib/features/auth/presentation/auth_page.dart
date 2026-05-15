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
      AuthMode.register => '注册并进入',
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
                        border: Border.all(color: Colors.white.withValues(alpha: 0.5)),
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
                            Text(headline, style: Theme.of(context).textTheme.headlineMedium),
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
                              selected: {_mode == AuthMode.reset ? AuthMode.login : _mode},
                              onSelectionChanged: (value) {
                                setState(() => _mode = value.first);
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
                                        prefixIcon: Icon(Icons.mark_email_read_outlined),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  SizedBox(
                                    height: 56,
                                    child: OutlinedButton(
                                      onPressed: _sendingCaptcha ? null : _sendCaptcha,
                                      style: OutlinedButton.styleFrom(
                                        foregroundColor: AppTheme.primaryBlue,
                                        side: const BorderSide(color: AppTheme.border),
                                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
                                      ),
                                      child: Text(_sendingCaptcha ? '发送中' : '获取验证码'),
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
                                  hintText: '输入昵称（可选）',
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
                              const SizedBox(height: 8),
                              const Text(
                                '密码至少 8 位，且包含大写字母、小写字母和数字',
                                style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                              ),
                              const SizedBox(height: 14),
                              const _Label('确认密码'),
                              const SizedBox(height: 8),
                              TextField(
                                controller: _confirmPasswordController,
                                obscureText: true,
                                decoration: const InputDecoration(
                                  hintText: '再次输入密码',
                                  prefixIcon: Icon(Icons.verified_user_outlined),
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
                                  onPressed: () => setState(() => _mode = AuthMode.reset),
                                  child: const Text('忘记密码'),
                                ),
                              ),
                            ] else if (_mode == AuthMode.reset) ...[
                              const SizedBox(height: 10),
                              Align(
                                alignment: Alignment.centerRight,
                                child: TextButton(
                                  onPressed: () => setState(() => _mode = AuthMode.login),
                                  child: const Text('返回登录'),
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
    final email = _emailController.text.trim();
    final emailError = _validateEmail(email);
    if (emailError != null) {
      _toast(emailError);
      return;
    }

    setState(() => _sendingCaptcha = true);
    try {
      final scene = _mode == AuthMode.reset ? 'reset_password' : 'register';
      await AppScope.of(context).authService.sendCaptcha(email, scene);
      _toast('验证码已发送，请注意查收邮箱');
    } on ApiException catch (error) {
      _toast(error.message);
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _sendingCaptcha = false);
    }
  }

  Future<void> _submit() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text;
    final captcha = _captchaController.text.trim();

    final emailError = _validateEmail(email);
    if (emailError != null) {
      _toast(emailError);
      return;
    }

    if (_mode != AuthMode.login && captcha.isEmpty) {
      _toast('请输入邮箱验证码');
      return;
    }

    final passwordError = _validatePassword(password);
    if (passwordError != null) {
      _toast(passwordError);
      return;
    }

    if (_mode == AuthMode.register && password != _confirmPasswordController.text) {
      _toast('两次输入的密码不一致');
      return;
    }

    setState(() => _submitting = true);
    try {
      final authService = AppScope.of(context).authService;
      switch (_mode) {
        case AuthMode.login:
          await authService.login(email: email, password: password);
          widget.onLogin();
          break;
        case AuthMode.register:
          await authService.register(email: email, password: password, captcha: captcha);
          final nickname = _nicknameController.text.trim();
          if (nickname.isNotEmpty) {
            await authService.updateProfile(nickname: nickname);
          }
          widget.onLogin();
          break;
        case AuthMode.reset:
          await authService.resetPassword(email: email, captcha: captcha, newPassword: password);
          if (mounted) setState(() => _mode = AuthMode.login);
          _toast('密码已更新，请重新登录');
          break;
      }
    } on ApiException catch (error) {
      _toast(error.message);
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String? _validateEmail(String email) {
    if (email.isEmpty) return '请输入邮箱';
    final valid = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email);
    if (!valid) return '邮箱格式不正确，请检查后重试';
    return null;
  }

  String? _validatePassword(String password) {
    if (password.isEmpty) return '请输入密码';
    if (password.length < 8) return '密码长度至少 8 位';
    if (password.length > 64) return '密码长度不能超过 64 位';
    if (!RegExp(r'[A-Z]').hasMatch(password)) return '密码必须包含至少 1 个大写字母';
    if (!RegExp(r'[a-z]').hasMatch(password)) return '密码必须包含至少 1 个小写字母';
    if (!RegExp(r'\d').hasMatch(password)) return '密码必须包含至少 1 个数字';
    return null;
  }

  void _toast(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}

class _Label extends StatelessWidget {
  const _Label(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(fontWeight: FontWeight.w700, color: AppTheme.textPrimary),
    );
  }
}
