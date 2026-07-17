import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/availability/data/availability_models.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/notifications/data/notification_models.dart';
import 'package:mawa3id/features/patient/data/patient_models.dart';

/// Fixtures mirror the exact field names of the backend DTO records.
void main() {
  test('AuthResponse.fromJson', () {
    final auth = AuthResponse.fromJson({
      'token': 'jwt',
      'tokenType': 'Bearer',
      'expiresInMs': 86400000,
      'refreshToken': 'refresh-1',
      'userId': 7,
      'username': 'sami',
      'email': 'a@b.c',
      'role': 'PATIENT',
    });
    expect(auth.token, 'jwt');
    expect(auth.tokenType, 'Bearer');
    expect(auth.expiresInMs, 86400000);
    expect(auth.refreshToken, 'refresh-1');
    expect(auth.userId, 7);
    expect(auth.username, 'sami');
    expect(auth.role, UserRole.patient);
  });

  test('AuthResponse.fromJson tolerates a missing refreshToken and email', () {
    final auth = AuthResponse.fromJson({
      'token': 'jwt',
      'tokenType': 'Bearer',
      'expiresInMs': 1000,
      'userId': 7,
      'username': 'sami',
      'role': 'PATIENT',
    });
    expect(auth.refreshToken, isNull);
    expect(auth.email, isNull);
  });

  test('MeResponse.fromJson', () {
    final me = MeResponse.fromJson({
      'userId': 9,
      'username': 'amal',
      'email': 'doc@x.y',
      'role': 'DOCTOR',
      'emailVerified': true,
      'phoneVerified': false,
    });
    expect(me.userId, 9);
    expect(me.username, 'amal');
    expect(me.role, UserRole.doctor);
    expect(me.emailVerified, isTrue);
    expect(me.phoneVerified, isFalse);
  });

  test('MeResponse.fromJson defaults verification flags to false', () {
    final me = MeResponse.fromJson(
        {'userId': 9, 'username': 'amal', 'email': 'doc@x.y', 'role': 'DOCTOR'});
    expect(me.emailVerified, isFalse);
    expect(me.phoneVerified, isFalse);
  });

  test('PatientResponse.fromJson parses date and tolerates a null one', () {
    final patient = PatientResponse.fromJson({
      'userId': 7,
      'email': 'p@x.y',
      'fullName': 'Sami Ben',
      'dateOfBirth': '1995-03-07',
      'patientCode': 'MW-ABC234',
    });
    expect(patient.fullName, 'Sami Ben');
    expect(patient.patientCode, 'MW-ABC234');
    expect(patient.dateOfBirth, DateTime(1995, 3, 7));

    final noDob = PatientResponse.fromJson({
      'userId': 7,
      'email': 'p@x.y',
      'fullName': 'Sami Ben',
      'dateOfBirth': null,
      'patientCode': 'MW-ABC234',
    });
    expect(noDob.dateOfBirth, isNull);
  });

  test('SpecialtyResponse.fromJson with null description', () {
    final specialty = SpecialtyResponse.fromJson(
        {'id': 1, 'name': 'Cardiology', 'description': null});
    expect(specialty.id, 1);
    expect(specialty.name, 'Cardiology');
    expect(specialty.description, isNull);
  });

  test('DoctorSummary.fromJson handles integer ratingAverage', () {
    final summary = DoctorSummary.fromJson({
      'userId': 3,
      'name': 'Dr. Amal',
      'specialtyName': 'Cardiology',
      'cabinetAddress': null,
      'ratingAverage': 4, // JSON integer, Dart double field
      'ratingCount': 12,
    });
    expect(summary.ratingAverage, 4.0);
    expect(summary.cabinetAddress, isNull);
  });

  test('DoctorResponse.fromJson with nested specialty and AUTO mode', () {
    final doctor = DoctorResponse.fromJson({
      'userId': 3,
      'email': 'doc@x.y',
      'name': 'Dr. Amal',
      'specialty': {'id': 1, 'name': 'Cardiology', 'description': 'Heart'},
      'cabinetAddress': '1 Rue X',
      'workingHours': 'Mon-Fri',
      'phone': '+216...',
      'bio': null,
      'acceptanceMode': 'AUTO',
      'slotDurationMinutes': 30,
      'ratingAverage': 4.5,
      'ratingCount': 12,
    });
    expect(doctor.specialty!.name, 'Cardiology');
    expect(doctor.acceptanceMode, AcceptanceMode.auto);
    expect(doctor.slotDurationMinutes, 30);
  });

  test('ReviewResponse.fromJson parses UTC instants', () {
    final review = ReviewResponse.fromJson({
      'id': 5,
      'appointmentId': 11,
      'doctorId': 3,
      'patientId': 7,
      'patientName': 'Sami',
      'rating': 5,
      'comment': 'Great',
      'createdAt': '2026-07-01T10:00:00Z',
      'updatedAt': null,
    });
    expect(review.rating, 5);
    expect(review.createdAt.isUtc, isTrue);
    expect(review.updatedAt, isNull);
  });

  test('AppointmentResponse.fromJson keeps wall-clock times unshifted', () {
    final appointment = AppointmentResponse.fromJson({
      'id': 11,
      'doctorId': 3,
      'doctorName': 'Dr. Amal',
      'patientId': 7,
      'patientName': 'Sami',
      'start': '2026-07-14T09:30:00',
      'end': '2026-07-14T10:00:00',
      'status': 'PENDING',
      'reason': 'checkup',
    });
    expect(appointment.start.hour, 9);
    expect(appointment.start.minute, 30);
    expect(appointment.start.isUtc, isFalse);
    expect(appointment.status, AppointmentStatus.pending);
  });

  test('SlotResponse.fromJson', () {
    final slot = SlotResponse.fromJson(
        {'start': '2026-07-14T09:00:00', 'end': '2026-07-14T09:30:00'});
    expect(slot.end.difference(slot.start), const Duration(minutes: 30));
  });

  test('AvailabilityRule round-trips and computes minutes', () {
    final rule = AvailabilityRule.fromJson({
      'dayOfWeek': 'WEDNESDAY',
      'startTime': '09:00:00',
      'endTime': '12:30:00',
    });
    expect(rule.dayOfWeek, Weekday.wednesday);
    expect(rule.startMinutes, 9 * 60);
    expect(rule.endMinutes, 12 * 60 + 30);
    expect(rule.toJson()['dayOfWeek'], 'WEDNESDAY');
  });

  test('time helpers parse HH:mm and HH:mm:ss and format HH:mm', () {
    expect(parseTimeToMinutes('08:15'), 495);
    expect(parseTimeToMinutes('08:15:00'), 495);
    expect(formatMinutesAsTime(495), '08:15');
  });

  test('NotificationResponse.fromJson with known and unknown types', () {
    final known = NotificationResponse.fromJson({
      'id': 1,
      'type': 'APPOINTMENT_ACCEPTED',
      'message': 'Accepted!',
      'appointmentId': 11,
      'read': false,
      'createdAt': '2026-07-13T08:00:00Z',
    });
    expect(known.type, NotificationType.appointmentAccepted);
    expect(known.read, isFalse);
    expect(known.createdAt.isUtc, isTrue);

    final future = NotificationResponse.fromJson({
      'id': 2,
      'type': 'SOMETHING_NEW',
      'message': '?',
      'appointmentId': null,
      'read': true,
      'createdAt': '2026-07-13T08:00:00Z',
    });
    expect(future.type, NotificationType.unknown);
  });
}
