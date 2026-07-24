// Store-screenshot harness. Drives the real app with a fixtured API (no backend)
// and captures each key screen, per locale, at the device's native resolution.
// Run via the screenshot driver:
//
//   flutter drive \
//     --driver=test_driver/screenshot_driver.dart \
//     --target=integration_test/screenshots_test.dart
//
// The driver writes the PNGs under build/screenshots/<locale>/<name>.png; CI maps
// them into the fastlane screenshot folders. See docs/store/SUBMISSION.md.
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:mawa3id/app/app.dart';
import 'package:mawa3id/app/router.dart';
import 'package:mawa3id/core/l10n/locale_controller.dart';
import 'package:mawa3id/core/network/dio_client.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';

import 'support/screenshot_fixtures.dart';

/// Lands the app logged-in without touching storage/network.
class _FixedSession extends SessionController {
  _FixedSession(this._session);
  final Session _session;
  @override
  Future<Session?> build() async => _session;
}

const _patient = Session(
  userId: 7,
  username: 'Sami Ben',
  email: 'sami@example.com',
  role: UserRole.patient,
  emailVerified: true,
  phoneVerified: true,
);

const _doctor = Session(
  userId: 3,
  username: 'Dr. Amal Bennani',
  email: 'amal@example.com',
  role: UserRole.doctor,
  emailVerified: true,
  phoneVerified: true,
);

const _locales = ['en', 'fr', 'ar'];

const _patientShots = <(String, String)>[
  ('/p/doctors', '01-doctors'),
  ('/p/doctors/3', '02-doctor-detail'),
  ('/p/doctors/3/book', '03-book-slot'),
  ('/p/appointments', '04-appointments'),
  ('/p/appointments/records', '05-records'),
  ('/p/notifications', '06-notifications'),
  ('/donate', '07-donate'),
];

const _doctorShots = <(String, String)>[
  ('/d/appointments', '08-agenda'),
  ('/d/availability', '09-availability'),
];

var _surfaceConverted = false;

Future<void> main() async {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('capture store screenshots', (tester) async {
    for (final locale in _locales) {
      await _capture(tester, binding, locale, _patient, _patientShots);
      await _capture(tester, binding, locale, _doctor, _doctorShots);
    }
  });
}

Future<void> _capture(
  WidgetTester tester,
  IntegrationTestWidgetsFlutterBinding binding,
  String locale,
  Session session,
  List<(String, String)> shots,
) async {
  final container = ProviderContainer(overrides: [
    dioProvider.overrideWithValue(fixtureDio()),
    sessionControllerProvider.overrideWith(() => _FixedSession(session)),
  ]);
  addTearDown(container.dispose);

  await tester.pumpWidget(
    UncontrolledProviderScope(container: container, child: const MawaApp()),
  );
  await container.read(localeControllerProvider.notifier).set(Locale(locale));
  await _settle(tester);

  // iOS requires converting the Flutter surface to an image once before any
  // screenshot; on Android takeScreenshot works directly.
  if (Platform.isIOS && !_surfaceConverted) {
    await binding.convertFlutterSurfaceToImage();
    _surfaceConverted = true;
    await tester.pump();
  }

  final router = container.read(routerProvider);
  for (final (route, name) in shots) {
    router.go(route);
    await _settle(tester);
    await binding.takeScreenshot('$locale/$name');
  }
}

/// Settle animations, tolerating a lingering indicator rather than failing the
/// whole capture run.
Future<void> _settle(WidgetTester tester) async {
  try {
    await tester.pumpAndSettle(const Duration(milliseconds: 120));
  } catch (_) {
    for (var i = 0; i < 6; i++) {
      await tester.pump(const Duration(milliseconds: 200));
    }
  }
}
