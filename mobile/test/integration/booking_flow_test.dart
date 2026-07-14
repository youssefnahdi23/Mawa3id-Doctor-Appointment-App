@Tags(['integration'])
library;

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/network/api_exception.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/appointments/data/appointment_repository.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mawa3id/features/availability/data/availability_models.dart';
import 'package:mawa3id/features/availability/data/availability_repository.dart';
import 'package:mawa3id/features/doctors/data/doctor_repository.dart';
import 'package:mawa3id/features/notifications/data/notification_models.dart';
import 'package:mawa3id/features/notifications/data/notification_repository.dart';

/// End-to-end flows against a real backend:
///   docker compose up -d --build
///   flutter test --tags integration --dart-define=API_BASE_URL=http://localhost:8080
///
/// The tests in this file run in order and share state (one doctor, two
/// patients — well under the 10/60s auth rate limit).
const _baseUrl = String.fromEnvironment('API_BASE_URL',
    defaultValue: 'http://localhost:8080');

Dio _dio([String? token]) => Dio(BaseOptions(
      baseUrl: _baseUrl,
      headers: token == null ? null : {'Authorization': 'Bearer $token'},
    ));

void main() {
  final suffix = DateTime.now().millisecondsSinceEpoch;
  late AuthResponse doctorAuth;
  late AuthResponse patientAuth;
  late Dio doctorDio;
  late Dio patientDio;
  late SlotResponse firstSlot;
  late AppointmentResponse booked;

  test('doctor registers and publishes weekly availability', () async {
    final anonymous = _dio();
    final specialties = await DoctorRepository(anonymous).specialties();
    expect(specialties, isNotEmpty,
        reason: 'backend seed data should include specialties');

    doctorAuth = await AuthRepository(anonymous).registerDoctor(
      email: 'it-doc-$suffix@example.test',
      password: 'password123',
      name: 'Dr Integration $suffix',
      specialtyId: specialties.first.id,
    );
    expect(doctorAuth.role, UserRole.doctor);
    doctorDio = _dio(doctorAuth.token);

    final rules = await AvailabilityRepository(doctorDio).updateMine([
      for (final day in Weekday.values)
        AvailabilityRule(dayOfWeek: day, startTime: '08:00', endTime: '18:00'),
    ]);
    expect(rules, hasLength(7));
  });

  test('patient registers, sees slots and books one', () async {
    patientAuth = await AuthRepository(_dio()).registerPatient(
      email: 'it-pat-$suffix@example.test',
      password: 'password123',
      fullName: 'Integration Patient',
      dateOfBirth: DateTime(1990, 1, 1),
    );
    patientDio = _dio(patientAuth.token);

    final now = DateTime.now();
    final tomorrow = DateTime(now.year, now.month, now.day + 1);
    final slots = await AppointmentRepository(patientDio).slots(
      doctorAuth.userId,
      from: tomorrow,
      to: DateTime(now.year, now.month, now.day + 7),
    );
    expect(slots, isNotEmpty);
    firstSlot = slots.first;

    booked = await AppointmentRepository(patientDio).book(
      doctorId: doctorAuth.userId,
      startTime: firstSlot.start,
      reason: 'integration test booking',
    );
    // New doctors default to MANUAL acceptance.
    expect(booked.status, AppointmentStatus.pending);
    expect(booked.start, firstSlot.start);
  });

  test('doctor sees the request, accepts it, patient gets notified',
      () async {
    final queue = await AppointmentRepository(doctorDio)
        .queue(status: AppointmentStatus.pending);
    expect(queue.content.map((a) => a.id), contains(booked.id));

    final accepted =
        await AppointmentRepository(doctorDio).accept(booked.id);
    expect(accepted.status, AppointmentStatus.accepted);

    final notifications =
        await NotificationRepository(patientDio).list();
    final accepteds = notifications.content
        .where((n) => n.type == NotificationType.appointmentAccepted);
    expect(accepteds.map((n) => n.appointmentId), contains(booked.id));
    expect(await NotificationRepository(patientDio).unreadCount(),
        greaterThan(0));
  });

  test('double-booking the same slot conflicts; cancelling frees it',
      () async {
    final rival = await AuthRepository(_dio()).registerPatient(
      email: 'it-pat2-$suffix@example.test',
      password: 'password123',
      fullName: 'Rival Patient',
      dateOfBirth: DateTime(1992, 2, 2),
    );
    final rivalDio = _dio(rival.token);

    await expectLater(
      AppointmentRepository(rivalDio).book(
        doctorId: doctorAuth.userId,
        startTime: firstSlot.start,
      ),
      throwsA(isA<ApiException>()
          .having((e) => e.kind, 'kind', ApiErrorKind.conflict)),
    );

    final cancelled =
        await AppointmentRepository(patientDio).cancel(booked.id);
    expect(cancelled.status, AppointmentStatus.cancelled);

    final rebooked = await AppointmentRepository(rivalDio).book(
      doctorId: doctorAuth.userId,
      startTime: firstSlot.start,
    );
    expect(rebooked.status, AppointmentStatus.pending);
  });
}
