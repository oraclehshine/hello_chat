import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/config/app_environment.dart';
import 'package:app/core/utils/asset_urls.dart';
import 'package:flutter/material.dart';

class AppAvatar extends StatelessWidget {
  const AppAvatar({
    super.key,
    required this.label,
    this.size = 48,
    this.imageUrl,
  });

  final String label;
  final double size;
  final String? imageUrl;

  @override
  Widget build(BuildContext context) {
    final initials = label.trim().isEmpty ? 'HC' : label.trim().substring(0, 1);
    final rawUrl = imageUrl?.trim() ?? '';
    final resolvedUrl = _resolveAvatarUrl(rawUrl);

    if (resolvedUrl.isNotEmpty) {
      return Container(
        width: size,
        height: size,
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(size * 0.38),
          color: Colors.white,
          border: Border.all(color: AppTheme.border),
        ),
        child: Image.network(
          resolvedUrl,
          fit: BoxFit.cover,
          errorBuilder: (context, error, stackTrace) =>
              _Fallback(initials: initials, size: size),
        ),
      );
    }

    return _Fallback(initials: initials, size: size);
  }

  String _resolveAvatarUrl(String url) {
    if (url.isEmpty) {
      return '';
    }
    if (url.startsWith('/api/') || url.startsWith('api/')) {
      final apiBase = Uri.parse(AppEnvironment.baseUrl);
      final origin = '${apiBase.scheme}://${apiBase.authority}';
      final normalized = url.startsWith('/') ? url : '/$url';
      return '$origin$normalized';
    }
    if (url.contains('/files/redirect?source=')) {
      return url;
    }
    return resolveAssetUrl(url);
  }
}

class _Fallback extends StatelessWidget {
  const _Fallback({required this.initials, required this.size});

  final String initials;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(size * 0.38),
        gradient: const LinearGradient(
          colors: [Color(0xFF67A5FF), AppTheme.primaryBlue],
        ),
      ),
      child: Text(
        initials.toUpperCase(),
        style: TextStyle(
          color: Colors.white,
          fontSize: size * 0.34,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}
