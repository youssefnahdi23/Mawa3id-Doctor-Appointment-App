import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/patient/data/patient_models.dart';
import 'package:mawa3id/features/patient/data/patient_repository.dart';
import 'package:mawa3id/features/patient/state/patient_providers.dart';
import 'package:mawa3id/features/patient/ui/patient_edit_profile_screen.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockPatientRepository extends Mock implements PatientRepository {}

final _patient = PatientResponse(
  userId: 7,
  email: 'p@x.y',
  fullName: 'Sami Ben',
  patientCode: 'MW-ABC234',
  dateOfBirth: DateTime(1995, 3, 7),
);

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  List<Override> overridesWith(PatientRepository repo) => [
        myPatientProfileProvider.overrideWith((ref) async => _patient),
        patientRepositoryProvider.overrideWithValue(repo),
      ];

  testWidgets('shows read-only identity and seeds the name', (tester) async {
    await tester.pumpWidget(wrapWithApp(const PatientEditProfileScreen(),
        overrides: overridesWith(_MockPatientRepository())));
    await tester.pumpAndSettle();

    expect(find.text('p@x.y'), findsOneWidget);
    expect(find.text('MW-ABC234'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Sami Ben'), findsOneWidget);
  });

  testWidgets('save posts the edited name through updateMe', (tester) async {
    final repo = _MockPatientRepository();
    when(() => repo.updateMe(
          fullName: any(named: 'fullName'),
          dateOfBirth: any(named: 'dateOfBirth'),
          phone: any(named: 'phone'),
        )).thenAnswer((_) async => _patient);

    await tester.pumpWidget(wrapWithApp(const PatientEditProfileScreen(),
        overrides: overridesWith(repo)));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextFormField, 'Sami Ben'), 'Sami Ben Updated');
    await tester.ensureVisible(find.text('Save'));
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    final captured = verify(() => repo.updateMe(
          fullName: captureAny(named: 'fullName'),
          dateOfBirth: any(named: 'dateOfBirth'),
          phone: any(named: 'phone'),
        )).captured;
    expect(captured.single, 'Sami Ben Updated');
  });
}
