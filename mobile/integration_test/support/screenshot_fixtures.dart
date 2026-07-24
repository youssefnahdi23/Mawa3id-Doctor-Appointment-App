// Canned API responses for the store-screenshot harness. A fake Dio
// HttpClientAdapter routes each request path to a fixture so the real app
// renders populated screens with no backend. Shapes mirror the backend DTOs
// (kept in sync with test/models/models_test.dart).
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// Two-digit zero-pad.
String _pad(int n) => n.toString().padLeft(2, '0');

/// Local wall-clock ISO string (no zone), matching the backend's LocalDateTime.
String _localDateTime(DateTime d) =>
    '${d.year}-${_pad(d.month)}-${_pad(d.day)}T${_pad(d.hour)}:${_pad(d.minute)}:00';

String _localDate(DateTime d) => '${d.year}-${_pad(d.month)}-${_pad(d.day)}';

const _specialties = [
  {'id': 1, 'name': 'Cardiology', 'description': 'Heart & vessels'},
  {'id': 2, 'name': 'Dermatology', 'description': 'Skin'},
  {'id': 3, 'name': 'Pediatrics', 'description': 'Children'},
  {'id': 4, 'name': 'General medicine', 'description': null},
];

Map<String, dynamic> _page(List<Object?> content, {int size = 20}) => {
      'content': content,
      'page': {
        'size': size,
        'number': 0,
        'totalElements': content.length,
        'totalPages': 1,
      },
    };

const _doctorsList = [
  {
    'userId': 3,
    'name': 'Dr. Amal Bennani',
    'specialtyName': 'Cardiology',
    'cabinetAddress': '12 Rue de la Santé, Casablanca',
    'ratingAverage': 4.8,
    'ratingCount': 42,
  },
  {
    'userId': 4,
    'name': 'Dr. Karim Idrissi',
    'specialtyName': 'Dermatology',
    'cabinetAddress': '5 Avenue Hassan II, Rabat',
    'ratingAverage': 4.6,
    'ratingCount': 27,
  },
  {
    'userId': 5,
    'name': 'Dr. Salma Haddad',
    'specialtyName': 'Pediatrics',
    'cabinetAddress': '30 Boulevard Zerktouni, Marrakech',
    'ratingAverage': 4.9,
    'ratingCount': 61,
  },
];

const _doctorDetail = {
  'userId': 3,
  'email': 'amal@example.com',
  'name': 'Dr. Amal Bennani',
  'specialty': {'id': 1, 'name': 'Cardiology', 'description': 'Heart & vessels'},
  'cabinetAddress': '12 Rue de la Santé, Casablanca',
  'workingHours': 'Mon–Fri, 09:00–17:00',
  'phone': '+212 600-000000',
  'bio': 'Cardiologist with 12 years of experience in preventive care.',
  'acceptanceMode': 'AUTO',
  'slotDurationMinutes': 30,
  'ratingAverage': 4.8,
  'ratingCount': 42,
};

Map<String, dynamic> _reviews() => _page([
      {
        'id': 5,
        'appointmentId': 11,
        'doctorId': 3,
        'patientId': 7,
        'patientName': 'Sami B.',
        'rating': 5,
        'comment': 'Very attentive and clear explanations.',
        'createdAt': '2026-07-01T10:00:00Z',
        'updatedAt': null,
      },
      {
        'id': 6,
        'appointmentId': 12,
        'doctorId': 3,
        'patientId': 8,
        'patientName': 'Nadia R.',
        'rating': 5,
        'comment': 'On time and very professional.',
        'createdAt': '2026-06-20T14:30:00Z',
        'updatedAt': null,
      },
    ]);

const _availability = [
  {'dayOfWeek': 'MONDAY', 'startTime': '09:00:00', 'endTime': '12:30:00'},
  {'dayOfWeek': 'MONDAY', 'startTime': '14:00:00', 'endTime': '17:00:00'},
  {'dayOfWeek': 'WEDNESDAY', 'startTime': '09:00:00', 'endTime': '13:00:00'},
  {'dayOfWeek': 'FRIDAY', 'startTime': '10:00:00', 'endTime': '16:00:00'},
];

/// Free slots for the next week so the slot picker always has content whatever
/// day it defaults to.
List<Map<String, String>> _slots() {
  final now = DateTime.now();
  final base = DateTime(now.year, now.month, now.day);
  final slots = <Map<String, String>>[];
  for (var day = 0; day < 7; day++) {
    for (final hour in [9, 10, 11, 14, 15, 16]) {
      final start = base.add(Duration(days: day, hours: hour));
      slots.add({
        'start': _localDateTime(start),
        'end': _localDateTime(start.add(const Duration(minutes: 30))),
      });
    }
  }
  return slots;
}

Map<String, dynamic> _patientAppointments() {
  final now = DateTime.now();
  final base = DateTime(now.year, now.month, now.day);
  Map<String, dynamic> appt(int id, int dayOffset, int hour, String status,
          String doctor, String? reason) =>
      {
        'id': id,
        'doctorId': 3,
        'doctorName': doctor,
        'patientId': 7,
        'patientName': 'Sami Ben',
        'start': _localDateTime(base.add(Duration(days: dayOffset, hours: hour))),
        'end': _localDateTime(
            base.add(Duration(days: dayOffset, hours: hour, minutes: 30))),
        'status': status,
        'reason': reason,
      };
  return _page([
    appt(21, 2, 10, 'ACCEPTED', 'Dr. Amal Bennani', 'Follow-up'),
    appt(22, 5, 14, 'PENDING', 'Dr. Karim Idrissi', 'Skin check'),
    appt(20, -6, 9, 'COMPLETED', 'Dr. Salma Haddad', 'Consultation'),
  ]);
}

Map<String, dynamic> _doctorAppointments() {
  final now = DateTime.now();
  final base = DateTime(now.year, now.month, now.day);
  Map<String, dynamic> appt(
          int id, int dayOffset, int hour, String status, String patient) =>
      {
        'id': id,
        'doctorId': 3,
        'doctorName': 'Dr. Amal Bennani',
        'patientId': id,
        'patientName': patient,
        'start': _localDateTime(base.add(Duration(days: dayOffset, hours: hour))),
        'end': _localDateTime(
            base.add(Duration(days: dayOffset, hours: hour, minutes: 30))),
        'status': status,
        'reason': null,
      };
  return _page([
    appt(31, 0, 9, 'ACCEPTED', 'Sami Ben'),
    appt(32, 0, 10, 'PENDING', 'Nadia Raji'),
    appt(33, 1, 11, 'ACCEPTED', 'Omar Alaoui'),
  ]);
}

Map<String, dynamic> _notifications() => _page([
      {
        'id': 1,
        'type': 'APPOINTMENT_ACCEPTED',
        'message': 'Your appointment with Dr. Amal Bennani was accepted.',
        'appointmentId': 21,
        'read': false,
        'createdAt': '2026-07-23T08:00:00Z',
      },
      {
        'id': 2,
        'type': 'APPOINTMENT_REMINDER',
        'message': 'Reminder: appointment tomorrow at 10:00.',
        'appointmentId': 21,
        'read': false,
        'createdAt': '2026-07-22T18:00:00Z',
      },
      {
        'id': 3,
        'type': 'APPOINTMENT_COMPLETED',
        'message': 'Your visit is complete. A medical record is available.',
        'appointmentId': 20,
        'read': true,
        'createdAt': '2026-07-18T09:30:00Z',
      },
    ]);

Map<String, dynamic> _records() {
  final now = DateTime.now();
  return _page([
    {
      'id': 5,
      'appointmentId': 20,
      'doctorId': 5,
      'doctorName': 'Dr. Salma Haddad',
      'patientId': 7,
      'patientName': 'Sami Ben',
      'visitStart': _localDateTime(now.subtract(const Duration(days: 6))),
      'diagnosis': 'Seasonal flu',
      'notes': 'Rest and fluids; recheck if fever persists.',
      'prescription': 'Paracetamol 500mg, as needed',
      'followUpDate': _localDate(now.add(const Duration(days: 1))),
      'createdAt': '2026-07-18T10:00:00Z',
      'updatedAt': null,
    },
  ]);
}

const _donationConfig = {
  'cardEnabled': true,
  'currency': 'usd',
  'minAmountMinor': 500,
  'patreonUrl': 'https://patreon.com/mawa3id',
  'konnectEnabled': false,
  'ribEnabled': false,
};

const _patientProfile = {
  'userId': 7,
  'email': 'sami@example.com',
  'fullName': 'Sami Ben',
  'dateOfBirth': '1995-03-07',
  'patientCode': 'MW-ABC234',
};

/// Resolves a request to a (statusCode, jsonBody) fixture. Order matters:
/// specific paths before their prefixes.
(int, Object) _resolve(RequestOptions options) {
  final path = options.uri.path;
  bool is_(String p) => path == p;
  bool startsWith(String p) => path.startsWith(p);

  if (is_('/api/specialties')) return (200, _specialties);
  if (is_('/api/doctors')) return (200, _doctorsList.let(_page));
  if (is_('/api/doctors/me')) return (200, _doctorDetail);
  if (is_('/api/doctors/me/availability')) return (200, _availability);
  if (path.endsWith('/reviews')) return (200, _reviews());
  if (path.endsWith('/availability')) return (200, _availability);
  if (path.endsWith('/slots')) return (200, _slots());
  if (path.endsWith('/record')) {
    return (200, (_records()['content'] as List).first as Object);
  }
  if (is_('/api/records/me')) return (200, _records());
  if (startsWith('/api/doctors/')) return (200, _doctorDetail);
  if (is_('/api/appointments/me')) return (200, _patientAppointments());
  if (is_('/api/appointments')) return (200, _doctorAppointments());
  if (is_('/api/notifications/unread-count')) return (200, {'count': 2});
  if (startsWith('/api/notifications')) return (200, _notifications());
  if (is_('/api/patients/me')) return (200, _patientProfile);
  if (is_('/api/donations/config')) return (200, _donationConfig);
  if (is_('/api/donations/me')) return (200, _page(const []));

  // Anything unmapped: an empty page / empty object keeps screens from erroring.
  return (200, <String, dynamic>{});
}

extension _Let<T> on T {
  R let<R>(R Function(T) f) => f(this);
}

/// A Dio adapter that answers from the fixtures instead of the network.
class FixtureAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(RequestOptions options,
      Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    final (status, body) = _resolve(options);
    return ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// A Dio wired to the fixtures, for overriding `dioProvider` in the harness.
Dio fixtureDio() {
  final dio = Dio(BaseOptions(baseUrl: 'http://screenshots.local'));
  dio.httpClientAdapter = FixtureAdapter();
  return dio;
}
