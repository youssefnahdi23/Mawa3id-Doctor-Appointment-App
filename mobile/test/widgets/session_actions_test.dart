import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/l10n/l10n.dart';
import 'package:mawa3id/core/widgets/session_actions.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';

import 'helpers.dart';

Widget _host(Session session) => ProviderScope(
      overrides: [
        sessionControllerProvider
            .overrideWith(() => FakeSessionController(session)),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(appBar: AppBar(actions: const [AccountMenuButton()])),
      ),
    );

void main() {
  const session = Session(
    userId: 1,
    username: 'sami',
    email: 'a@b.c',
    role: UserRole.patient,
    emailVerified: true,
    phoneVerified: false,
  );

  testWidgets('account menu shows verification status per contact',
      (tester) async {
    await tester.pumpWidget(_host(session));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.account_circle_outlined));
    await tester.pumpAndSettle();

    // Verified email, unverified phone.
    expect(find.text('Verified'), findsOneWidget);
    expect(find.text('Not verified'), findsOneWidget);
    expect(find.text('Sign out of all devices'), findsOneWidget);
  });

  testWidgets('sign out of all devices asks for confirmation first',
      (tester) async {
    await tester.pumpWidget(_host(session));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.account_circle_outlined));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Sign out of all devices'));
    await tester.pumpAndSettle();

    // The destructive action is gated behind a confirm dialog.
    expect(find.text('Sign out everywhere'), findsOneWidget);
  });
}
