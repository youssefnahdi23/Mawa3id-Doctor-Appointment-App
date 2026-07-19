/// A provider-neutral push message. Mirrors the backend payload: a display
/// [title]/[body] plus a [data] map carrying `type` and (optionally)
/// `appointmentId` for deep-linking.
class PushMessage {
  const PushMessage({this.title, this.body, this.data = const {}});

  final String? title;
  final String? body;
  final Map<String, String> data;

  String? get type => data['type'];

  /// The appointment this message deep-links to, if any.
  int? get appointmentId {
    final raw = data['appointmentId'];
    return raw == null ? null : int.tryParse(raw);
  }
}

/// Seam over the FCM + local-notification plugins so [PushService] — and its
/// tests — never touch the platform singletons directly. The production
/// implementation is `FirebasePushMessaging`; tests inject a fake.
abstract class PushMessaging {
  /// Ask the OS for notification permission (iOS always; Android 13+). Returns
  /// whether permission is granted.
  Future<bool> requestPermission();

  /// The current FCM registration token, or null if unavailable/not configured.
  Future<String?> getToken();

  /// Emits a new token whenever the platform rotates it.
  Stream<String> get onTokenRefresh;

  /// Messages received while the app is in the foreground.
  Stream<PushMessage> get onForegroundMessage;

  /// Fired when the user taps a notification that opened the app from the
  /// background.
  Stream<PushMessage> get onMessageOpenedApp;

  /// The message that launched the app from a terminated state via a tap, if any.
  Future<PushMessage?> getInitialMessage();

  /// Drop the token (on logout) so this device stops receiving pushes.
  Future<void> deleteToken();

  /// Show a local notification for a foreground message (FCM does not display
  /// these automatically).
  Future<void> showNotification(PushMessage message);
}
