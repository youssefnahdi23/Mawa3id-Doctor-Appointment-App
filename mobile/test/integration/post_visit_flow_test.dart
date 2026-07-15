@Tags(['integration'])
library;

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/appointments/data/appointment_repository.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mawa3id/features/availability/data/availability_models.dart';
import 'package:mawa3id/features/availability/data/availability_repository.dart';
import 'package:mawa3id/features/doctors/data/doctor_repository.dart';
import 'package:mawa3id/features/records/data/record_models.dart';
import 'package:mawa3id/features/records/data/record_repository.dart';
import 'package:mawa3id/features/reviews/data/review_repository.dart';

/// End-to-end post-visit flow against a real backend:
///   docker compose up -d --build
///   flutter test --tags integration --dart-define=API_BASE_URL=http://localhost:8080
///
/// Tests run in order and share state (one doctor, one patient).
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
  late AppointmentResponse booked;

  test('setup: doctor with availability, patient books and doctor accepts',
      () async {
    final anonymous = _dio();
    final specialties = await DoctorRepository(anonymous).specialties();
    doctorAuth = await AuthRepository(anonymous).registerDoctor(
      email: 'pv-doc-$suffix@example.test',
      password: 'password123',
      name: 'Dr Post-Visit $suffix',
      specialtyId: specialties.first.id,
    );
    doctorDio = _dio(doctorAuth.token);
    await AvailabilityRepository(doctorDio).updateMine([
      for (final day in Weekday.values)
        AvailabilityRule(dayOfWeek: day, startTime: '08:00', endTime: '18:00'),
    ]);

    patientAuth = await AuthRepository(_dio()).registerPatient(
      email: 'pv-pat-$suffix@example.test',
      password: 'password123',
      fullName: 'Post-Visit Patient',
      dateOfBirth: DateTime(1990, 1, 1),
    );
    patientDio = _dio(patientAuth.token);

    final now = DateTime.now();
    final slots = await AppointmentRepository(patientDio).slots(
      doctorAuth.userId,
      from: DateTime(now.year, now.month, now.day + 1),
      to: DateTime(now.year, now.month, now.day + 7),
    );
    booked = await AppointmentRepository(patientDio)
        .book(doctorId: doctorAuth.userId, startTime: slots.first.start);
    final accepted =
        await AppointmentRepository(doctorDio).accept(booked.id);
    expect(accepted.status, AppointmentStatus.accepted);
  });

  test('doctor attaches a record which auto-completes the appointment',
      () async {
    final record = await RecordRepository(doctorDio).create(
      booked.id,
      RecordRequest(
        diagnosis: 'Seasonal flu',
        notes: 'Rest and fluids',
        prescription: 'Paracetamol 500mg',
        followUpDate: DateTime.now().add(const Duration(days: 7)),
      ),
    );
    expect(record.diagnosis, 'Seasonal flu');

    // The appointment is now COMPLETED and appears in the patient's history.
    final history = await RecordRepository(patientDio).mine();
    expect(history.content.map((r) => r.appointmentId), contains(booked.id));

    // The patient can read the same record for the appointment.
    final fetched = await RecordRepository(patientDio).get(booked.id);
    expect(fetched, isNotNull);
    expect(fetched!.prescription, 'Paracetamol 500mg');
  });

  test('patient reviews the visit and the doctor rating updates', () async {
    final before = await DoctorRepository(patientDio).byId(doctorAuth.userId);

    await ReviewRepository(patientDio)
        .create(booked.id, const ReviewRequest(rating: 5, comment: 'Great'));

    final after = await DoctorRepository(patientDio).byId(doctorAuth.userId);
    expect(after.ratingCount, before.ratingCount + 1);
    expect(after.ratingAverage, greaterThan(0));

    // Editing the review is idempotent for the count.
    await ReviewRepository(patientDio)
        .update(booked.id, const ReviewRequest(rating: 4));
    final edited = await DoctorRepository(patientDio).byId(doctorAuth.userId);
    expect(edited.ratingCount, after.ratingCount);
  });
}
