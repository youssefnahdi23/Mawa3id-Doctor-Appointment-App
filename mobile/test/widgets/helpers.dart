import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mawa3id/core/l10n/l10n.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';

/// Session controller stub that skips storage/network entirely.
class FakeSessionController extends SessionController {
  FakeSessionController([this.session]);

  final Session? session;

  @override
  Future<Session?> build() async => session;
}

Widget wrapWithApp(
  Widget child, {
  List<Override> overrides = const [],
  Locale? locale,
}) {
  return ProviderScope(
    overrides: [
      // Never let widget tests touch secure storage or the network via the
      // real session controller.
      sessionControllerProvider.overrideWith(FakeSessionController.new),
      ...overrides,
    ],
    child: MaterialApp(
      locale: locale,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: child,
    ),
  );
}
