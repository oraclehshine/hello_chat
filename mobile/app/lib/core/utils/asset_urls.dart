import 'package:app/core/config/app_environment.dart';

String resolveAssetUrl(String sourceUrl) {
  if (sourceUrl.isEmpty) {
    return sourceUrl;
  }
  final encoded = Uri.encodeQueryComponent(sourceUrl);
  return '${AppEnvironment.baseUrl}/files/redirect?source=$encoded';
}
