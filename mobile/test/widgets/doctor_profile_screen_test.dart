import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/doctors/data/doctor_repository.dart';
import 'package:mawa3id/features/doctors/state/doctors_providers.dart';
import 'package:mawa3id/features/doctors/ui/doctor_profile_screen.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockDoctorRepository extends Mock implements DoctorRepository {}

const _doctor = DoctorResponse(
  userId: 3,
  email: 'doc@x.y',
  name: 'Dr. Amal',
  specialty: SpecialtyResponse(id: 1, name: 'Cardiology'),
  cabinetAddress: '1 Rue X',
  workingHours: 'Mon-Fri',
  phone: '+212600',
  bio: 'Heart specialist',
  acceptanceMode: AcceptanceMode.manual,
  slotDurationMinutes: 30,
  ratingAverage: 4.5,
  ratingCount: 2,
);

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));
  setUpAll(() => registerFallbackValue(AcceptanceMode.manual));

  List<Override> overridesWith(DoctorRepository repo) => [
        myDoctorProfileProvider.overrideWith((ref) async => _doctor),
        specialtiesProvider.overrideWith(
            (ref) async => const [SpecialtyResponse(id: 1, name: 'Cardiology')]),
        doctorRepositoryProvider.overrideWithValue(repo),
      ];

  testWidgets('seeds the form from the loaded profile', (tester) async {
    await tester.pumpWidget(wrapWithApp(const DoctorProfileScreen(),
        overrides: overridesWith(_MockDoctorRepository())));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextFormField, 'Dr. Amal'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Heart specialist'),
        findsOneWidget);
    expect(find.widgetWithText(TextFormField, '30'), findsOneWidget);
  });

  testWidgets('save posts the edited values through updateMe',
      (tester) async {
    final repo = _MockDoctorRepository();
    when(() => repo.updateMe(
          name: any(named: 'name'),
          specialtyId: any(named: 'specialtyId'),
          cabinetAddress: any(named: 'cabinetAddress'),
          workingHours: any(named: 'workingHours'),
          phone: any(named: 'phone'),
          bio: any(named: 'bio'),
          acceptanceMode: any(named: 'acceptanceMode'),
          slotDurationMinutes: any(named: 'slotDurationMinutes'),
          cnamConventionne: any(named: 'cnamConventionne'),
          governorate: any(named: 'governorate'),
          consultationFee: any(named: 'consultationFee'),
          languages: any(named: 'languages'),
        )).thenAnswer((_) async => _doctor);

    await tester.pumpWidget(wrapWithApp(const DoctorProfileScreen(),
        overrides: overridesWith(repo)));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextFormField, 'Dr. Amal'), 'Dr. Amal Updated');
    await tester.ensureVisible(find.text('Save'));
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    final captured = verify(() => repo.updateMe(
          name: captureAny(named: 'name'),
          specialtyId: any(named: 'specialtyId'),
          cabinetAddress: any(named: 'cabinetAddress'),
          workingHours: any(named: 'workingHours'),
          phone: any(named: 'phone'),
          bio: any(named: 'bio'),
          acceptanceMode: any(named: 'acceptanceMode'),
          slotDurationMinutes: captureAny(named: 'slotDurationMinutes'),
          cnamConventionne: any(named: 'cnamConventionne'),
          governorate: any(named: 'governorate'),
          consultationFee: any(named: 'consultationFee'),
          languages: any(named: 'languages'),
        )).captured;
    expect(captured[0], 'Dr. Amal Updated');
    expect(captured[1], 30);
  });

  testWidgets('an out-of-range slot duration blocks the save', (tester) async {
    final repo = _MockDoctorRepository();

    await tester.pumpWidget(wrapWithApp(const DoctorProfileScreen(),
        overrides: overridesWith(repo)));
    await tester.pumpAndSettle();

    await tester.enterText(
        find.widgetWithText(TextFormField, '30'), '3');
    await tester.ensureVisible(find.text('Save'));
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    verifyNever(() => repo.updateMe(
          name: any(named: 'name'),
          specialtyId: any(named: 'specialtyId'),
          cabinetAddress: any(named: 'cabinetAddress'),
          workingHours: any(named: 'workingHours'),
          phone: any(named: 'phone'),
          bio: any(named: 'bio'),
          acceptanceMode: any(named: 'acceptanceMode'),
          slotDurationMinutes: any(named: 'slotDurationMinutes'),
          cnamConventionne: any(named: 'cnamConventionne'),
          governorate: any(named: 'governorate'),
          consultationFee: any(named: 'consultationFee'),
          languages: any(named: 'languages'),
        ));
  });
}
