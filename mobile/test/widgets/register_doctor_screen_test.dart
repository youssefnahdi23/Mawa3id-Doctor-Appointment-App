import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/network/api_exception.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mawa3id/features/auth/ui/register_doctor_screen.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/doctors/state/doctors_providers.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockAuthRepository extends Mock implements AuthRepository {}

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

  testWidgets('a taken username surfaces an inline error on the field',
      (tester) async {
    final repo = _MockAuthRepository();
    when(() => repo.registerDoctor(
          email: any(named: 'email'),
          password: any(named: 'password'),
          name: any(named: 'name'),
          specialtyId: any(named: 'specialtyId'),
          cabinetAddress: any(named: 'cabinetAddress'),
          phone: any(named: 'phone'),
          username: any(named: 'username'),
        )).thenThrow(const ApiException(
      kind: ApiErrorKind.conflict,
      statusCode: 409,
      message: 'Username already taken',
    ));

    await tester.pumpWidget(wrapWithApp(
      const RegisterDoctorScreen(),
      overrides: [
        ...overrides,
        authRepositoryProvider.overrideWithValue(repo),
      ],
    ));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextFormField, 'Name'), 'Dr. A');
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Email'), 'doc@x.y');
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Password'), 'secret123');
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Username (optional)'), 'taken');

    await tester.tap(find.text('Specialty'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cardiology').last);
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Create account'));
    await tester.tap(find.text('Create account'));
    await tester.pumpAndSettle();

    expect(find.text('That username is already taken'), findsOneWidget);
  });
}
