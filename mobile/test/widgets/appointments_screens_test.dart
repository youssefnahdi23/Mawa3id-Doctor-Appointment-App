import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/models/page_response.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/appointments/data/appointment_repository.dart';
import 'package:mawa3id/features/appointments/ui/doctor_appointments_screen.dart';
import 'package:mawa3id/features/appointments/ui/patient_appointments_screen.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockAppointmentRepository extends Mock
    implements AppointmentRepository {}

AppointmentResponse _appointment(AppointmentStatus status) =>
    AppointmentResponse(
      id: 11,
      doctorId: 3,
      doctorName: 'Dr. Amal',
      patientId: 7,
      patientName: 'Sami',
      start: DateTime(2026, 7, 14, 9, 30),
      end: DateTime(2026, 7, 14, 10, 0),
      status: status,
      reason: 'checkup',
    );

PageResponse<AppointmentResponse> _page(
        List<AppointmentResponse> items) =>
    PageResponse(
      content: items,
      page: PageMeta(
          size: 20,
          number: 0,
          totalElements: items.length,
          totalPages: 1),
    );

void main() {
  late _MockAppointmentRepository repository;

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    repository = _MockAppointmentRepository();
  });

  group('PatientAppointmentsScreen', () {
    void stubMine(List<AppointmentResponse> items) {
      when(() => repository.mine(
              status: any(named: 'status'), page: any(named: 'page')))
          .thenAnswer((_) async => _page(items));
    }

    testWidgets('cancel flows through the confirmation dialog',
        (tester) async {
      stubMine([_appointment(AppointmentStatus.accepted)]);
      when(() => repository.cancel(11)).thenAnswer(
          (_) async => _appointment(AppointmentStatus.cancelled));

      await tester.pumpWidget(
          wrapWithApp(const PatientAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      expect(find.text('Dr. Amal'), findsOneWidget);
      await tester.tap(find.text('Cancel'));
      await tester.pumpAndSettle();
      expect(find.text('Cancel appointment?'), findsOneWidget);

      await tester.tap(find.text('Cancel appointment'));
      await tester.pumpAndSettle();

      verify(() => repository.cancel(11)).called(1);
    });

    testWidgets('dismissing the dialog does not cancel', (tester) async {
      stubMine([_appointment(AppointmentStatus.pending)]);

      await tester.pumpWidget(
          wrapWithApp(const PatientAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Cancel'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Keep it'));
      await tester.pumpAndSettle();

      verifyNever(() => repository.cancel(any()));
    });

    testWidgets('completed appointments offer record + review, not cancel',
        (tester) async {
      stubMine([_appointment(AppointmentStatus.completed)]);

      await tester.pumpWidget(
          wrapWithApp(const PatientAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      expect(find.text('Cancel'), findsNothing);
      expect(find.text('View record'), findsOneWidget);
      expect(find.text('Review'), findsOneWidget);
    });
  });

  group('DoctorAppointmentsScreen', () {
    void stubQueue(List<AppointmentResponse> items) {
      when(() => repository.queue(
              status: any(named: 'status'), page: any(named: 'page')))
          .thenAnswer((_) async => _page(items));
    }

    testWidgets('PENDING shows accept/reject only', (tester) async {
      stubQueue([_appointment(AppointmentStatus.pending)]);

      await tester.pumpWidget(
          wrapWithApp(const DoctorAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      expect(find.text('Accept'), findsOneWidget);
      expect(find.text('Reject'), findsOneWidget);
      expect(find.text('Complete'), findsNothing);
    });

    testWidgets('ACCEPTED shows complete/no-show/reschedule/cancel',
        (tester) async {
      stubQueue([_appointment(AppointmentStatus.accepted)]);

      await tester.pumpWidget(
          wrapWithApp(const DoctorAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      expect(find.text('Complete with notes'), findsOneWidget);
      expect(find.text('No-show'), findsOneWidget);
      expect(find.text('Reschedule'), findsOneWidget);
      expect(find.text('Cancel'), findsOneWidget);
      expect(find.text('Accept'), findsNothing);
    });

    testWidgets('tapping No-show calls the repository', (tester) async {
      stubQueue([_appointment(AppointmentStatus.accepted)]);
      when(() => repository.noShow(11))
          .thenAnswer((_) async => _appointment(AppointmentStatus.noShow));

      await tester.pumpWidget(
          wrapWithApp(const DoctorAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      stubQueue([_appointment(AppointmentStatus.noShow)]);
      await tester.tap(find.text('No-show'));
      await tester.pumpAndSettle();

      verify(() => repository.noShow(11)).called(1);
      expect(find.text('No-show'), findsOneWidget); // now the status chip
    });

    testWidgets('COMPLETED shows view/edit record', (tester) async {
      stubQueue([_appointment(AppointmentStatus.completed)]);

      await tester.pumpWidget(
          wrapWithApp(const DoctorAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      expect(find.text('View / edit record'), findsOneWidget);
    });

    testWidgets('tapping Accept calls the repository and refetches',
        (tester) async {
      stubQueue([_appointment(AppointmentStatus.pending)]);
      when(() => repository.accept(11)).thenAnswer(
          (_) async => _appointment(AppointmentStatus.accepted));

      await tester.pumpWidget(
          wrapWithApp(const DoctorAppointmentsScreen(), overrides: [
        appointmentRepositoryProvider.overrideWithValue(repository),
      ]));
      await tester.pumpAndSettle();

      stubQueue([_appointment(AppointmentStatus.accepted)]);
      await tester.tap(find.text('Accept'));
      await tester.pumpAndSettle();

      verify(() => repository.accept(11)).called(1);
      expect(find.text('Complete with notes'), findsOneWidget);
    });
  });
}
