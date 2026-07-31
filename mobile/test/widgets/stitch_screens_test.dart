import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/appointments/state/appointments_providers.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/doctors/state/doctors_providers.dart';
import 'package:mawa3id/features/home/ui/home_screen.dart';
import 'package:mawa3id/features/patient/data/patient_models.dart';
import 'package:mawa3id/features/patient/state/patient_providers.dart';
import 'package:mawa3id/features/patient/ui/patient_profile_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

/// Empty-but-resolved fakes so the dashboards render without touching network.
class _FakeAppts extends PatientAppointmentsController {
  @override
  Future<AppointmentListData> build() async =>
      const AppointmentListData(items: [], isLast: true, nextPage: 1);
}

class _FakeDoctors extends DoctorListController {
  @override
  Future<DoctorListData> build() async =>
      const DoctorListData(items: [], isLast: true, nextPage: 1);
}

final _patient = PatientResponse(
  userId: 7,
  email: 'p@x.y',
  fullName: 'Sami Ben',
  patientCode: 'MW-ABC234',
);

List<Override> _overrides() => [
      myPatientProfileProvider.overrideWith((ref) async => _patient),
      patientAppointmentsProvider.overrideWith(_FakeAppts.new),
      doctorListControllerProvider.overrideWith(_FakeDoctors.new),
      specialtiesProvider.overrideWith((ref) async => <SpecialtyResponse>[]),
    ];

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  testWidgets('Home greets the patient and shows dashboard sections',
      (tester) async {
    await tester.pumpWidget(
        wrapWithApp(const HomeScreen(), overrides: _overrides()));
    await tester.pumpAndSettle();

    expect(find.text('Hello, Sami'), findsOneWidget);
    expect(find.text('Top doctors'), findsOneWidget);
    expect(find.text('Upcoming appointment'), findsOneWidget);
  });

  testWidgets('Profile overview shows identity and account settings',
      (tester) async {
    await tester.pumpWidget(
        wrapWithApp(const PatientProfileScreen(), overrides: _overrides()));
    await tester.pumpAndSettle();

    expect(find.text('Sami Ben'), findsOneWidget);
    expect(find.text('Account settings'), findsOneWidget);
    expect(find.text('Edit profile'), findsOneWidget);
    // The log-out row sits at the bottom of the lazy list; scroll to it.
    await tester.scrollUntilVisible(find.text('Log out'), 200);
    expect(find.text('Log out'), findsOneWidget);
  });
}
