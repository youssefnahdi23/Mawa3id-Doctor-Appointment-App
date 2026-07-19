import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/data/auth_models.dart';
import '../../features/auth/state/session_controller.dart';
import '../../features/notifications/data/device_repository.dart';
import '../../features/notifications/state/notifications_controller.dart';
import '../../features/notifications/state/unread_count_controller.dart';
import '../navigation/navigator_key.dart';
import 'push_messaging.dart';
import 'push_service.dart';

const _channelId = 'mawa3id_default';
const _channelName = 'Notifications';

/// Background/terminated data-message handler. Must be a top-level function.
/// The system tray already renders notification messages, so this is a no-op
/// kept registered so background delivery doesn't crash.
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // Intentionally empty.
}

/// Production [PushMessaging] backed by Firebase Cloud Messaging plus
/// flutter_local_notifications. Every entry point is guarded so that when no
/// Firebase config is present (e.g. local/dev without `google-services.json`)
/// the app still runs — push simply becomes inert.
class FirebasePushMessaging implements PushMessaging {
  final FlutterLocalNotificationsPlugin _local = FlutterLocalNotificationsPlugin();
  Future<bool>? _ready;

  Future<bool> _ensureReady() {
    return _ready ??= _initialize();
  }

  Future<bool> _initialize() async {
    try {
      await Firebase.initializeApp();
      FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
      const initSettings = InitializationSettings(
        android: AndroidInitializationSettings('@mipmap/ic_launcher'),
        iOS: DarwinInitializationSettings(),
      );
      await _local.initialize(initSettings);
      await _local
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.createNotificationChannel(const AndroidNotificationChannel(
            _channelId,
            _channelName,
            importance: Importance.high,
          ));
      return true;
    } catch (error, stack) {
      debugPrint('Push disabled — Firebase unavailable: $error\n$stack');
      return false;
    }
  }

  @override
  Future<bool> requestPermission() async {
    if (!await _ensureReady()) return false;
    try {
      await _local
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.requestNotificationsPermission();
      final settings = await FirebaseMessaging.instance.requestPermission();
      final status = settings.authorizationStatus;
      return status == AuthorizationStatus.authorized ||
          status == AuthorizationStatus.provisional;
    } catch (_) {
      return false;
    }
  }

  @override
  Future<String?> getToken() async {
    if (!await _ensureReady()) return null;
    try {
      return await FirebaseMessaging.instance.getToken();
    } catch (_) {
      return null;
    }
  }

  @override
  Stream<String> get onTokenRefresh => FirebaseMessaging.instance.onTokenRefresh;

  @override
  Stream<PushMessage> get onForegroundMessage =>
      FirebaseMessaging.onMessage.map(_toPushMessage);

  @override
  Stream<PushMessage> get onMessageOpenedApp =>
      FirebaseMessaging.onMessageOpenedApp.map(_toPushMessage);

  @override
  Future<PushMessage?> getInitialMessage() async {
    if (!await _ensureReady()) return null;
    try {
      final message = await FirebaseMessaging.instance.getInitialMessage();
      return message == null ? null : _toPushMessage(message);
    } catch (_) {
      return null;
    }
  }

  @override
  Future<void> deleteToken() async {
    if (!await _ensureReady()) return;
    try {
      await FirebaseMessaging.instance.deleteToken();
    } catch (_) {
      // best-effort
    }
  }

  @override
  Future<void> showNotification(PushMessage message) async {
    if (!await _ensureReady()) return;
    try {
      await _local.show(
        message.hashCode,
        message.title ?? 'Mawa3id',
        message.body ?? '',
        const NotificationDetails(
          android: AndroidNotificationDetails(
            _channelId,
            _channelName,
            importance: Importance.high,
            priority: Priority.high,
          ),
          iOS: DarwinNotificationDetails(),
        ),
      );
    } catch (_) {
      // best-effort
    }
  }

  static PushMessage _toPushMessage(RemoteMessage message) => PushMessage(
        title: message.notification?.title,
        body: message.notification?.body,
        data: message.data
            .map((key, value) => MapEntry(key, value?.toString() ?? '')),
      );
}

/// The app-wide push service, wired to FCM, the in-app feed refresh, and
/// go_router deep-linking. Overridden in tests with a fake.
final pushServiceProvider = Provider<PushService>((ref) {
  final platform =
      defaultTargetPlatform == TargetPlatform.iOS ? 'IOS' : 'ANDROID';
  final service = PushService(
    messaging: FirebasePushMessaging(),
    repository: ref.watch(deviceRepositoryProvider),
    platform: platform,
    onForeground: (_) {
      // Reflect the freshly-arrived notification in the feed and badge.
      ref.invalidate(notificationsControllerProvider);
      ref.read(unreadCountProvider.notifier).refresh();
    },
    onOpen: (message) => _openFromPush(ref, message),
  );
  ref.onDispose(service.dispose);
  return service;
});

/// Deep-link a notification tap to the relevant screen for the current role.
void _openFromPush(Ref ref, PushMessage message) {
  final context = rootNavigatorKey.currentContext;
  if (context == null) return;
  final role = ref.read(sessionControllerProvider).valueOrNull?.role;
  final prefix = role == UserRole.doctor ? '/d' : '/p';
  final appointmentId = message.appointmentId;
  if (appointmentId != null) {
    context.go('$prefix/appointments/$appointmentId/record');
  } else {
    context.go('$prefix/notifications');
  }
}
