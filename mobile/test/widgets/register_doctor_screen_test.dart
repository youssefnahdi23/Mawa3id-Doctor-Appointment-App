import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/ui/register_doctor_screen.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/doctors/state/doctors_providers.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  final overrides = [
    specialtiesProvider.overrideWith((ref) async => const [
          SpecialtyResponse(id: 1, name: 'Cardiology'),
          SpecialtyResponse(id: 2, name: 'Dermatology'),
        ]),
  ];

  testWidgets('requires a specialty before submitting', (tester) async {
    await tester.pumpWidget(
        wrapWithApp(const RegisterDoctorScreen(), overrides: overrides));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextFormField, 'Name'), 'Dr. A');
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Email'), 'doc@x.y');
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Password'), 'secret123');
    await tester.ensureVisible(find.text('Create account'));
    await tester.tap(find.text('Create account'));
    await tester.pumpAndSettle();

    expect(find.text('Choose a specialty'), findsOneWidget);
  });

  testWidgets('lists the fetched specialties in the dropdown',
      (tester) async {
    await tester.pumpWidget(
        wrapWithApp(const RegisterDoctorScreen(), overrides: overrides));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Specialty'));
    await tester.pumpAndSettle();

    expect(find.text('Cardiology'), findsWidgets);
    expect(find.text('Dermatology'), findsWidgets);
  });
}
