import 'dart:ui';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

final localeControllerProvider =
    NotifierProvider<LocaleController, Locale?>(LocaleController.new);

/// Persisted app locale; `null` follows the system locale.
class LocaleController extends Notifier<Locale?> {
  static const _prefsKey = 'app_locale';

  @override
  Locale? build() {
    _restore();
    return null;
  }

  Future<void> _restore() async {
    final prefs = await SharedPreferences.getInstance();
    final code = prefs.getString(_prefsKey);
    if (code != null) state = Locale(code);
  }

  Future<void> set(Locale? locale) async {
    state = locale;
    final prefs = await SharedPreferences.getInstance();
    if (locale == null) {
      await prefs.remove(_prefsKey);
    } else {
      await prefs.setString(_prefsKey, locale.languageCode);
    }
  }
}
