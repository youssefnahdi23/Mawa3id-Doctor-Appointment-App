import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Decouples the dio layer from the session controller: the interceptor
/// fires [forceLogout] on an unexpected 401 and the session controller
/// listens, avoiding a construction cycle between the two providers.
class AuthEvents {
  final _forcedLogout = StreamController<void>.broadcast();

  Stream<void> get onForcedLogout => _forcedLogout.stream;

  void forceLogout() => _forcedLogout.add(null);

  void dispose() => _forcedLogout.close();
}

final authEventsProvider = Provider<AuthEvents>((ref) {
  final events = AuthEvents();
  ref.onDispose(events.dispose);
  return events;
});
