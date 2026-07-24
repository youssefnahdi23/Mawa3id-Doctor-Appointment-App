import 'dart:io' show Platform;

import 'package:flutter/foundation.dart';

class AppConfig {
  AppConfig._();

  static const _definedBaseUrl = String.fromEnvironment('API_BASE_URL');

  /// Backend origin. Override with
  /// `flutter run --dart-define=API_BASE_URL=http://host:8080`.
  ///
  /// Defaults target a backend on the development machine: the Android
  /// emulator reaches the host via 10.0.2.2, everything else via localhost.
  static String get apiBaseUrl {
    if (_definedBaseUrl.isNotEmpty) return _definedBaseUrl;
    if (!kIsWeb && Platform.isAndroid) return 'http://10.0.2.2:8080';
    return 'http://localhost:8080';
  }

  static const _definedLegalBaseUrl = String.fromEnvironment('LEGAL_BASE_URL');

  /// Origin that serves the hosted `/legal/*` pages (privacy policy + terms).
  /// Defaults to [apiBaseUrl] — in production both point at the real HTTPS domain
  /// — but can be overridden with `--dart-define=LEGAL_BASE_URL=https://...`.
  static String get legalBaseUrl =>
      _definedLegalBaseUrl.isNotEmpty ? _definedLegalBaseUrl : apiBaseUrl;

  /// URL of the hosted privacy policy / terms page for the given [document]
  /// (`privacy` or `terms`), localized to [languageCode] (en/fr/ar).
  static Uri legalUrl(String document, String languageCode) => Uri.parse(
        '$legalBaseUrl/legal/$document?lang=$languageCode',
      );
}
