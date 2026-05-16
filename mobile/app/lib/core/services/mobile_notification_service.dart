import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class MobileNotificationService {
  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  bool _ready = false;

  Future<void> initialize() async {
    if (_ready) return;
    const android = AndroidInitializationSettings('@mipmap/ic_launcher');
    const ios = DarwinInitializationSettings();
    const settings = InitializationSettings(android: android, iOS: ios);
    await _plugin.initialize(settings);
    await _plugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    await _plugin
        .resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(alert: true, badge: true, sound: true);
    _ready = true;
  }

  Future<void> notifyIncomingMessage({
    required String title,
    required String body,
    required int id,
  }) async {
    if (!_ready) {
      await initialize();
    }
    const androidDetails = AndroidNotificationDetails(
      'hello_chat_messages',
      'Hello Chat Messages',
      channelDescription: 'Incoming private and group messages',
      importance: Importance.high,
      priority: Priority.high,
      playSound: true,
      enableVibration: true,
    );
    const iosDetails = DarwinNotificationDetails(presentAlert: true, presentBadge: true, presentSound: true);
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);
    await _plugin.show(id, title, body, details);
  }
}
