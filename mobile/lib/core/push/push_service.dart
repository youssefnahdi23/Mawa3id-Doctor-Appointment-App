import 'dart:async';

import '../../features/notifications/data/device_repository.dart';
import 'push_messaging.dart';

/// Orchestrates push registration on top of the [PushMessaging] seam: requests
/// permission, registers the FCM token with the backend, keeps it fresh on
/// rotation, and routes incoming messages. Deliberately free of any plugin
/// imports so it is fully unit-testable with a fake [PushMessaging].
class PushService {
  PushService({
    required PushMessaging messaging,
    required DeviceRepository repository,
    required String platform,
    void Function(PushMessage message)? onForeground,
    void Function(PushMessage message)? onOpen,
  })  : _messaging = messaging,
        _repository = repository,
        _platform = platform,
        _onForeground = onForeground,
        _onOpen = onOpen;

  final PushMessaging _messaging;
  final DeviceRepository _repository;
  final String _platform;
  final void Function(PushMessage message)? _onForeground;
  final void Function(PushMessage message)? _onOpen;

  final List<StreamSubscription<dynamic>> _subscriptions = [];
  bool _listenersWired = false;
  String? _currentToken;

  /// Request permission, register this device's token, and wire message
  /// listeners. Safe to call more than once (login + authenticated boot); the
  /// registration is an idempotent upsert and listeners are wired once.
  Future<void> registerCurrentDevice() async {
    _wireListeners();
    final granted = await _messaging.requestPermission();
    if (!granted) return;
    final token = await _messaging.getToken();
    if (token == null) return;
    _currentToken = token;
    await _repository.register(token: token, platform: _platform);

    // Handle a notification tap that launched the app from a terminated state.
    final initial = await _messaging.getInitialMessage();
    if (initial != null) {
      _onOpen?.call(initial);
    }
  }

  /// Remove this device's token on logout (best-effort, both server and client).
  Future<void> unregister() async {
    final token = _currentToken ?? await _messaging.getToken();
    if (token != null) {
      await _repository.unregister(token);
    }
    await _messaging.deleteToken();
    _currentToken = null;
  }

  void _wireListeners() {
    if (_listenersWired) return;
    _listenersWired = true;
    _subscriptions.add(_messaging.onTokenRefresh.listen(_reregister));
    _subscriptions.add(_messaging.onForegroundMessage.listen((message) {
      unawaited(_messaging.showNotification(message));
      _onForeground?.call(message);
    }));
    _subscriptions.add(
      _messaging.onMessageOpenedApp.listen((message) => _onOpen?.call(message)),
    );
  }

  Future<void> _reregister(String token) async {
    _currentToken = token;
    try {
      await _repository.register(token: token, platform: _platform);
    } catch (_) {
      // A failed re-registration is non-fatal; the next login retries.
    }
  }

  void dispose() {
    for (final subscription in _subscriptions) {
      subscription.cancel();
    }
    _subscriptions.clear();
  }
}
