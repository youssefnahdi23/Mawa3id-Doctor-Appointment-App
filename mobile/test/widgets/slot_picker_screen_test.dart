import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mawa3id/core/l10n/l10n.dart';
import 'package:mawa3id/core/network/api_exception.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/appointments/data/appointment_repository.dart';
import 'package:mawa3id/features/appointments/ui/slot_picker_screen.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockAppointmentRepository extends Mock
    implements AppointmentRepository {}

void main() {
  late _MockAppointmentRepository repository;
  late DateTime slotStart;

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    repository = _MockAppointmentRepository();
    final tomorrow = DateTime.now().add(const Duration(days: 1));
    slotStart = DateTime(tomorrow.year, tomorrow.month, tomorrow.day, 9, 0);
    when(() => repository.slots(3,
            from: any(named: 'from'), to: any(named: 'to')))
        .thenAnswer((_) async => [
              SlotResponse(
                  start: slotStart,
                  end: slotStart.add(const Duration(minutes: 30))),
            ]);
  });

  Widget app() {
    final router = GoRouter(initialLocation: '/book', routes: [
      GoRoute(
          path: '/book',
          builder: (_, __) => const SlotPickerScreen(doctorId: 3)),
      GoRoute(
          path: '/p/appointments',
          builder: (_, __) =>
              const Scaffold(body: Text('appointments-screen'))),
    ]);
    return ProviderScope(
      overrides: [
        sessionControllerProvider.overrideWith(FakeSessionController.new),
        appointmentRepositoryProvider.overrideWithValue(repository),
      ],
      child: MaterialApp.router(
        routerConfig: router,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
      ),
    );
  }

  testWidgets('pick → confirm books the slot and lands on appointments',
      (tester) async {
    when(() => repository.book(
            doctorId: 3,
            startTime: slotStart,
            reason: any(named: 'reason')))
        .thenAnswer((_) async => AppointmentResponse(
              id: 11,
              doctorId: 3,
              doctorName: 'Dr. Amal',
              patientId: 7,
              patientName: 'Sami',
              start: slotStart,
              end: slotStart.add(const Duration(minutes: 30)),
              status: AppointmentStatus.accepted,
              reason: null,
            ));

    await tester.pumpWidget(app());
    await tester.pumpAndSettle();

    await tester.tap(find.text('09:00'));
    await tester.pumpAndSettle();
    expect(find.text('Confirm booking'), findsOneWidget);

    await tester.tap(find.text('Book this slot'));
    await tester.pumpAndSettle();

    verify(() => repository.book(
        doctorId: 3,
        startTime: slotStart,
        reason: any(named: 'reason'))).called(1);
    expect(find.text('appointments-screen'), findsOneWidget);
    expect(find.text('Appointment confirmed!'), findsOneWidget);
  });

  testWidgets('a 409 shows the slot-taken error and stays on the sheet',
      (tester) async {
    when(() => repository.book(
            doctorId: 3,
            startTime: slotStart,
            reason: any(named: 'reason')))
        .thenThrow(const ApiException(
            kind: ApiErrorKind.conflict, statusCode: 409));

    await tester.pumpWidget(app());
    await tester.pumpAndSettle();

    await tester.tap(find.text('09:00'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Book this slot'));
    await tester.pumpAndSettle();

    expect(
        find.text('This slot was just taken. Pick another one.'),
        findsOneWidget);
    expect(find.text('appointments-screen'), findsNothing);
  });
}
