import 'package:app/app/theme/app_theme.dart';
import 'package:app/core/utils/asset_urls.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class AttachmentMessageBody extends StatelessWidget {
  const AttachmentMessageBody({
    super.key,
    required this.messageType,
    required this.content,
    this.fileName,
    this.fileSize,
    required this.mine,
  });

  final String messageType;
  final String content;
  final String? fileName;
  final int? fileSize;
  final bool mine;

  @override
  Widget build(BuildContext context) {
    final textColor = mine ? Colors.white : AppTheme.textPrimary;
    final assetUrl = content.isNotEmpty ? resolveAssetUrl(content) : '';

    if (messageType == 'image' && assetUrl.isNotEmpty) {
      return GestureDetector(
        onTap: () => _openImagePreview(context, assetUrl),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Hero(
                tag: assetUrl,
                child: Image.network(
                  assetUrl,
                  width: 220,
                  height: 220,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => Container(
                    width: 220,
                    height: 220,
                    color: Colors.black12,
                    alignment: Alignment.center,
                    child: Text(
                      '图片加载失败',
                      style: TextStyle(color: textColor),
                    ),
                  ),
                ),
              ),
              if ((fileName ?? '').isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: Text(
                    fileName!,
                    style: TextStyle(
                      color: textColor,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
            ],
          ),
        ),
      );
    }

    if (messageType == 'file') {
      return InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: assetUrl.isEmpty ? null : () => _openFile(assetUrl),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.insert_drive_file_rounded,
              color: textColor,
            ),
            const SizedBox(width: 10),
            Flexible(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    (fileName ?? '').isNotEmpty ? fileName! : '附件',
                    style: TextStyle(
                      color: textColor,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _formatSize(fileSize),
                    style: TextStyle(
                      color: textColor.withValues(alpha: 0.82),
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Icon(
              Icons.open_in_new_rounded,
              size: 18,
              color: textColor.withValues(alpha: 0.86),
            ),
          ],
        ),
      );
    }

    return Text(
      content,
      style: TextStyle(
        color: textColor,
        height: 1.45,
      ),
    );
  }

  void _openImagePreview(BuildContext context, String imageUrl) {
    Navigator.of(context).push(
      PageRouteBuilder<void>(
        opaque: false,
        pageBuilder: (context, animation, secondaryAnimation) {
          return Scaffold(
            backgroundColor: Colors.black,
            body: SafeArea(
              child: Stack(
                children: [
                  Center(
                    child: InteractiveViewer(
                      minScale: 0.8,
                      maxScale: 4,
                      child: Hero(
                        tag: imageUrl,
                        child: Image.network(
                          imageUrl,
                          fit: BoxFit.contain,
                        ),
                      ),
                    ),
                  ),
                  Positioned(
                    top: 12,
                    right: 12,
                    child: IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close_rounded, color: Colors.white),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Future<void> _openFile(String fileUrl) async {
    final uri = Uri.parse(fileUrl);
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  String _formatSize(int? bytes) {
    if (bytes == null || bytes <= 0) return '未知大小';
    if (bytes < 1024) return '${bytes}B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)}KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)}MB';
  }
}
