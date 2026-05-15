import 'dart:io' show Platform;

class AppEnvironment {
  static const String mode = String.fromEnvironment(
    'APP_ENV',
    defaultValue: 'dev',
  );

  static const String apiScheme = String.fromEnvironment(
    'API_SCHEME',
    defaultValue: '',
  );

  static const String apiWsScheme = String.fromEnvironment(
    'API_WS_SCHEME',
    defaultValue: '',
  );

  static const String apiHost = String.fromEnvironment(
    'API_HOST',
    defaultValue: '',
  );

  static const String apiPort = String.fromEnvironment(
    'API_PORT',
    defaultValue: '444',
  );

  static bool get isProd => mode == 'prod';

  static String get resolvedApiScheme {
    if (apiScheme.isNotEmpty) {
      return apiScheme;
    }
    return isProd ? 'https' : 'http';
  }

  static String get resolvedWsScheme {
    if (apiWsScheme.isNotEmpty) {
      return apiWsScheme;
    }
    return isProd ? 'wss' : 'ws';
  }

  static String get defaultHost {
    if (apiHost.isNotEmpty) {
      return apiHost;
    }
    if (!isProd && Platform.isAndroid) {
      return '10.0.2.2';
    }
    return 'localhost';
  }

  static String get portSegment => apiPort.isEmpty ? '' : ':$apiPort';

  static String get baseUrl =>
      '$resolvedApiScheme://$defaultHost$portSegment/api/v1';

  static String get webSocketUrl =>
      '$resolvedWsScheme://$defaultHost$portSegment/ws/chat';
}
