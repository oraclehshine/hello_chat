import 'package:app/app/app_scope.dart';
import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/models/user_profile.dart';
import 'package:app/core/network/api_exception.dart';
import 'package:app/shared/widgets/app_avatar.dart';
import 'package:app/shared/widgets/glass_card.dart';
import 'package:app/shared/widgets/section_header.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key, required this.onLogout});

  final VoidCallback onLogout;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  late Future<UserProfile> _future;
  bool _updatingAvatar = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _future = AppScope.of(context).authService.getMe();
  }

  Future<void> _reload() async {
    setState(() {
      _future = AppScope.of(context).authService.getMe();
    });
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 84, 20, 120),
      children: [
        SectionHeader(
          title: '我的',
          action: IconButton(
            onPressed: _reload,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ),
        const SizedBox(height: 18),
        FutureBuilder<UserProfile>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return GlassCard(child: Text(snapshot.error.toString()));
            }
            final profile = snapshot.data!;
            return Column(
              children: [
                GlassCard(
                  child: Column(
                    children: [
                      Stack(
                        clipBehavior: Clip.none,
                        children: [
                          AppAvatar(
                            label: profile.nickname,
                            size: 78,
                            imageUrl: profile.avatarUrl,
                          ),
                          Positioned(
                            right: -2,
                            bottom: -2,
                            child: Material(
                              color: Colors.transparent,
                              child: InkWell(
                                borderRadius: BorderRadius.circular(999),
                                onTap: _updatingAvatar
                                    ? null
                                    : () => _changeAvatar(profile),
                                child: Container(
                                  width: 30,
                                  height: 30,
                                  decoration: BoxDecoration(
                                    color: AppTheme.primaryBlue,
                                    borderRadius: BorderRadius.circular(999),
                                    border: Border.all(
                                      color: Colors.white,
                                      width: 2,
                                    ),
                                  ),
                                  child: _updatingAvatar
                                      ? const Padding(
                                          padding: EdgeInsets.all(7),
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                            valueColor:
                                                AlwaysStoppedAnimation<Color>(
                                                  Colors.white,
                                                ),
                                          ),
                                        )
                                      : const Icon(
                                          Icons.camera_alt_rounded,
                                          color: Colors.white,
                                          size: 14,
                                        ),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      Text(
                        profile.nickname,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 6),
                      Text(
                        profile.signature?.isNotEmpty == true
                            ? profile.signature!
                            : '还没有填写个性签名。',
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: _StatBlock(
                              label: '邮箱',
                              value: profile.email,
                            ),
                          ),
                          Expanded(
                            child: _StatBlock(
                              label: '电话',
                              value: profile.phone?.isNotEmpty == true
                                  ? profile.phone!
                                  : '未设置',
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                GlassCard(
                  child: Column(
                    children: [
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: Container(
                          width: 44,
                          height: 44,
                          decoration: BoxDecoration(
                            color: AppTheme.primaryBlue.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: const Icon(
                            Icons.edit_outlined,
                            color: AppTheme.primaryBlue,
                          ),
                        ),
                        title: const Text(
                          '编辑个人资料',
                          style: TextStyle(
                            fontWeight: FontWeight.w800,
                            color: AppTheme.textPrimary,
                          ),
                        ),
                        trailing: const Icon(Icons.chevron_right_rounded),
                        onTap: () => _showEditSheet(profile),
                      ),
                      const Divider(height: 18),
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.lock_reset_rounded),
                        title: const Text('修改密码'),
                        trailing: const Icon(Icons.chevron_right_rounded),
                        onTap: _showChangePasswordSheet,
                      ),
                      const Divider(height: 18),
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.alternate_email_rounded),
                        title: const Text('更换邮箱'),
                        subtitle: Text(profile.email),
                        trailing: const Icon(Icons.chevron_right_rounded),
                        onTap: () => _showUpdateEmailSheet(profile),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                ElevatedButton.icon(
                  onPressed: () async {
                    final scope = AppScope.of(context);
                    await scope.chatSocketService.disconnect();
                    await scope.authService.logout();
                    if (!mounted) return;
                    widget.onLogout();
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFF15B5B),
                  ),
                  icon: const Icon(Icons.logout_rounded),
                  label: const Text('退出登录'),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  void _showEditSheet(UserProfile profile) {
    final nicknameController = TextEditingController(text: profile.nickname);
    final signatureController = TextEditingController(
      text: profile.signature ?? '',
    );
    final phoneController = TextEditingController(text: profile.phone ?? '');

    showModalBottomSheet<void>(
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
                  '编辑资料',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nicknameController,
                  decoration: const InputDecoration(hintText: '昵称'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: signatureController,
                  decoration: const InputDecoration(hintText: '个性签名'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: phoneController,
                  decoration: const InputDecoration(hintText: '联系电话'),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    const Spacer(),
                    FilledButton(
                      onPressed: () async {
                        try {
                          await AppScope.of(context).authService.updateProfile(
                            nickname: nicknameController.text.trim(),
                            signature: signatureController.text.trim(),
                            phone: phoneController.text.trim(),
                          );
                          navigator.pop();
                          if (!mounted) return;
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

  void _showChangePasswordSheet() {
    final oldController = TextEditingController();
    final newController = TextEditingController();
    final confirmController = TextEditingController();

    showModalBottomSheet<void>(
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
                  '修改密码',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: oldController,
                  obscureText: true,
                  decoration: const InputDecoration(hintText: '当前密码'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: newController,
                  obscureText: true,
                  decoration: const InputDecoration(hintText: '新密码'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: confirmController,
                  obscureText: true,
                  decoration: const InputDecoration(hintText: '确认新密码'),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    const Spacer(),
                    FilledButton(
                      onPressed: () async {
                        final newPassword = newController.text.trim();
                        if (newPassword != confirmController.text.trim()) {
                          messenger.showSnackBar(
                            const SnackBar(content: Text('两次输入的新密码不一致')),
                          );
                          return;
                        }
                        try {
                          await AppScope.of(context).authService.changePassword(
                            oldPassword: oldController.text.trim(),
                            newPassword: newPassword,
                          );
                          navigator.pop();
                          if (!mounted) return;
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('密码已更新')),
                          );
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

  void _showUpdateEmailSheet(UserProfile profile) {
    final emailController = TextEditingController(text: profile.email);
    final captchaController = TextEditingController();
    final rootMessenger = ScaffoldMessenger.of(context);
    final authService = AppScope.of(context).authService;
    var sending = false;

    showModalBottomSheet<void>(
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
                      '更换邮箱',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: emailController,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(hintText: '新邮箱'),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: captchaController,
                            decoration: const InputDecoration(hintText: '验证码'),
                          ),
                        ),
                        const SizedBox(width: 10),
                        OutlinedButton(
                          onPressed: sending
                              ? null
                              : () async {
                                  final email = emailController.text.trim();
                                  if (email.isEmpty) return;
                                  setSheetState(() => sending = true);
                                  try {
                                    final captcha = await authService
                                        .sendCaptcha(email, 'modify_email');
                                    captchaController.text = captcha;
                                  } on ApiException catch (error) {
                                    messenger.showSnackBar(
                                      SnackBar(content: Text(error.message)),
                                    );
                                  } finally {
                                    setSheetState(() => sending = false);
                                  }
                                },
                          child: Text(sending ? '发送中' : '获取验证码'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        const Spacer(),
                        FilledButton(
                          onPressed: () async {
                            try {
                              await authService.updateEmail(
                                email: emailController.text.trim(),
                                captcha: captchaController.text.trim(),
                              );
                              navigator.pop();
                              if (!mounted) return;
                              await _reload();
                              if (!mounted) return;
                              rootMessenger.showSnackBar(
                                const SnackBar(content: Text('邮箱已更新')),
                              );
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
              );
            },
          ),
        );
      },
    );
  }

  Future<void> _changeAvatar(UserProfile profile) async {
    final scope = AppScope.of(context);
    final messenger = ScaffoldMessenger.of(context);
    final hasPermission = await _requestMediaPermission();
    if (!hasPermission) {
      if (!mounted) return;
      messenger.showSnackBar(const SnackBar(content: Text('未授予相册权限，无法更换头像')));
      return;
    }

    final picked = await FilePicker.platform.pickFiles(
      type: FileType.image,
      allowMultiple: false,
    );
    final file = (picked == null || picked.files.isEmpty)
        ? null
        : picked.files.first;
    if (file == null || file.path == null) {
      return;
    }

    setState(() {
      _updatingAvatar = true;
    });

    try {
      final upload = await scope.fileService.uploadFile(
        filePath: file.path!,
        fileName: file.name,
        scene: 'avatar',
      );
      await scope.authService.updateProfile(avatarUrl: upload.fileUrl);
      if (!mounted) return;
      await _reload();
      if (!mounted) return;
      messenger.showSnackBar(const SnackBar(content: Text('头像已更新')));
    } on ApiException catch (error) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(error.message)));
    } catch (_) {
      if (!mounted) return;
      messenger.showSnackBar(const SnackBar(content: Text('头像更新失败，请稍后重试')));
    } finally {
      if (mounted) {
        setState(() {
          _updatingAvatar = false;
        });
      }
    }
  }

  Future<bool> _requestMediaPermission() async {
    final photos = await Permission.photos.request();
    if (photos.isGranted || photos.isLimited) {
      return true;
    }
    final storage = await Permission.storage.request();
    return storage.isGranted;
  }
}

class _StatBlock extends StatelessWidget {
  const _StatBlock({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w800,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Text(label),
      ],
    );
  }
}
