import 'package:app/core/config/app_environment.dart';
import 'package:app/core/services/mobile_notification_service.dart';
import 'package:app/core/services/file_service.dart';
import 'package:app/core/services/auth_service.dart';
import 'package:app/core/realtime/chat_socket_service.dart';
import 'package:app/core/services/chat_service.dart';
import 'package:app/core/services/friend_service.dart';
import 'package:app/core/services/group_service.dart';
import 'package:app/core/services/moment_service.dart';
import 'package:app/core/storage/session_store.dart';
import 'package:app/core/network/api_client.dart';
import 'package:flutter/widgets.dart';

class AppScope extends InheritedWidget {
  AppScope({
    super.key,
    required super.child,
    required this.sessionStore,
  })  : apiClient = ApiClient(
          baseUrl: AppEnvironment.baseUrl,
          sessionStore: sessionStore,
        ),
        authService = AuthService(
          ApiClient(
            baseUrl: AppEnvironment.baseUrl,
            sessionStore: sessionStore,
          ),
          sessionStore,
        ),
        chatService = ChatService(
          ApiClient(
            baseUrl: AppEnvironment.baseUrl,
            sessionStore: sessionStore,
          ),
        ),
        friendService = FriendService(
          ApiClient(
            baseUrl: AppEnvironment.baseUrl,
            sessionStore: sessionStore,
          ),
        ),
        groupService = GroupService(
          ApiClient(
            baseUrl: AppEnvironment.baseUrl,
            sessionStore: sessionStore,
          ),
        ),
        momentService = MomentService(
          ApiClient(
            baseUrl: AppEnvironment.baseUrl,
            sessionStore: sessionStore,
          ),
        ),
        fileService = FileService(
          ApiClient(
            baseUrl: AppEnvironment.baseUrl,
            sessionStore: sessionStore,
          ),
        ),
        chatSocketService = ChatSocketService(sessionStore);

  final SessionStore sessionStore;
  final ApiClient apiClient;
  final AuthService authService;
  final ChatService chatService;
  final FriendService friendService;
  final GroupService groupService;
  final MomentService momentService;
  final FileService fileService;
  final ChatSocketService chatSocketService;
  final MobileNotificationService notificationService = MobileNotificationService();

  static AppScope of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AppScope>();
    assert(scope != null, 'AppScope not found in widget tree.');
    return scope!;
  }

  @override
  bool updateShouldNotify(AppScope oldWidget) => sessionStore != oldWidget.sessionStore;
}
