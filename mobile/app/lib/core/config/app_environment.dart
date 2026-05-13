import 'dart:io' show Platform;

class AppEnvironment {
  static const String mode = String.fromEnvironment(
    'APP_ENV',
    defaultValue: 'dev',
  );

  static const String apiHost = String.fromEnvironment(
    'API_HOST',
    defaultValue: '',
  );

  static const String apiPort = String.fromEnvironment(
    'API_PORT',
    defaultValue: '8083',
  );

  static bool get isProd => mode == 'prod';

  static String get defaultHost {
    if (apiHost.isNotEmpty) {
      return apiHost;
    }
    if (!isProd && Platform.isAndroid) {
      return '10.0.2.2';
    }
    return 'localhost';
  }

  static String get baseUrl => 'http://$defaultHost:$apiPort/api/v1';

  static String get webSocketUrl => 'ws://$defaultHost:$apiPort/ws/chat';
}
