import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/state/session_controller.dart';
import '../network/dio_client.dart';
import 'locale_controller.dart';

const _supported = {'en', 'fr', 'ar'};

/// The app language to report to the backend: the chosen locale, or the platform
/// locale when following the system, clamped to a supported code.
String _effectiveLanguage(Locale? locale) {
  final code = locale?.languageCode ??
      WidgetsBinding.instance.platformDispatcher.locale.languageCode;
  return _supported.contains(code) ? code : 'en';
}

/// Keeps the backend's per-user `preferredLanguage` in sync with the app locale so
/// server-sent notifications — including push to the *other* party — are localized.
/// Best-effort: fires whenever the session or locale changes (and once on start),
/// and quietly no-ops when signed out. Keep it alive by watching it from the app root.
final languageSyncProvider = Provider<void>((ref) {
  Future<void> sync() async {
    final session = ref.read(sessionControllerProvider).valueOrNull;
    if (session == null) return;
    final code = _effectiveLanguage(ref.read(localeControllerProvider));
    try {
      await ref.read(dioProvider).put<void>(
        '/api/users/me/language',
        data: {'language': code},
      );
    } catch (_) {
      // Non-critical: a failed sync just means notifications use the last-known language.
    }
  }

  ref.listen(sessionControllerProvider, (_, __) => sync());
  ref.listen(localeControllerProvider, (_, __) => sync());
  sync();
});
