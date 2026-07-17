import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mawa3id/features/auth/ui/forgot_password_screen.dart';
import 'package:mawa3id/features/auth/ui/reset_password_screen.dart';
import 'package:mawa3id/features/auth/ui/verify_email_screen.dart';
import 'package:mawa3id/features/auth/ui/verify_phone_screen.dart';
import 'package:mocktail/mocktail.dart';

import 'helpers.dart';

class _MockAuthRepository extends Mock implements AuthRepository {}

void main() {
  testWidgets('forgot-password screen renders the identifier form',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(const ForgotPasswordScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Reset your password'), findsOneWidget);
    expect(find.text('Email, username, or phone'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Send code'), findsOneWidget);
  });

  testWidgets('reset-password screen asks for code and a new password',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(const ResetPasswordScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Reset code'), findsOneWidget);
    expect(find.text('New password'), findsOneWidget);
  });

  testWidgets('verify-phone screen renders the phone field', (tester) async {
    await tester.pumpWidget(wrapWithApp(const VerifyPhoneScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Phone number'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Send code'), findsOneWidget);
  });

  testWidgets('verify-email reveals the code field after requesting a code',
      (tester) async {
    final repository = _MockAuthRepository();
    when(() => repository.requestEmailVerification())
        .thenAnswer((_) async {});

    await tester.pumpWidget(wrapWithApp(
      const VerifyEmailScreen(),
      overrides: [authRepositoryProvider.overrideWithValue(repository)],
    ));
    await tester.pumpAndSettle();

    // The code field only appears once a code has been requested.
    expect(find.text('Verification code'), findsNothing);

    await tester.tap(find.widgetWithText(FilledButton, 'Send code'));
    await tester.pumpAndSettle();

    expect(find.text('Verification code'), findsOneWidget);
    verify(() => repository.requestEmailVerification()).called(1);
  });
}
