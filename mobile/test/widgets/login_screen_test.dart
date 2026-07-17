import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/ui/login_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  testWidgets('shows validation errors instead of submitting an empty form',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(const LoginScreen()));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle();

    // Both the identifier and password fields flag the empty submission.
    expect(find.text('This field is required'), findsNWidgets(2));
  });

  testWidgets('offers both registration entry points and password recovery',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(const LoginScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Create a patient account'), findsOneWidget);
    expect(find.text('Join as a doctor'), findsOneWidget);
    expect(find.text('Forgot password?'), findsOneWidget);
  });

  testWidgets('renders RTL with Arabic strings under the ar locale',
      (tester) async {
    await tester.pumpWidget(
        wrapWithApp(const LoginScreen(), locale: const Locale('ar')));
    await tester.pumpAndSettle();

    expect(find.text('تسجيل الدخول'), findsOneWidget);
    final context = tester.element(find.byType(LoginScreen));
    expect(Directionality.of(context), TextDirection.rtl);
  });
}
