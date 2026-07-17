import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/doctors/data/doctor_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

final _doctorJson = {
  'userId': 3,
  'email': 'doc@x.y',
  'name': 'Dr. Amal',
  'specialty': {'id': 1, 'name': 'Cardiology', 'description': null},
  'cabinetAddress': '1 Rue X',
  'workingHours': 'Mon-Fri',
  'phone': '+212600',
  'bio': 'Heart specialist',
  'acceptanceMode': 'AUTO',
  'slotDurationMinutes': 20,
  'ratingAverage': 4.5,
  'ratingCount': 2,
};

Response<Map<String, dynamic>> _okDoctor() => Response(
    requestOptions: RequestOptions(), statusCode: 200, data: _doctorJson);

final _pageJson = {
  'content': [
    {
      'userId': 3,
      'name': 'Dr. Amal',
      'specialtyName': 'Cardiology',
      'cabinetAddress': null,
      'ratingAverage': 4.5,
      'ratingCount': 2,
    }
  ],
  'page': {'size': 20, 'number': 0, 'totalElements': 1, 'totalPages': 1},
};

void main() {
  late _MockDio dio;
  late DoctorRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = DoctorRepository(dio);
  });

  test('specialties() parses the plain list payload', () async {
    when(() => dio.get<List<dynamic>>(any())).thenAnswer((_) async => Response(
          requestOptions: RequestOptions(),
          statusCode: 200,
          data: [
            {'id': 1, 'name': 'Cardiology', 'description': null},
          ],
        ));

    final specialties = await repository.specialties();

    expect(specialties.single.name, 'Cardiology');
    verify(() => dio.get<List<dynamic>>('/api/specialties')).called(1);
  });

  test('list() forwards filters and omits absent ones', () async {
    when(() => dio.get<Map<String, dynamic>>(any(),
            queryParameters: any(named: 'queryParameters')))
        .thenAnswer((_) async => Response(
            requestOptions: RequestOptions(),
            statusCode: 200,
            data: _pageJson));

    final page = await repository.list(specialtyId: 2, query: 'am', page: 1);
    expect(page.content.single.name, 'Dr. Amal');

    var captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[0], '/api/doctors');
    expect(captured[1],
        {'specialtyId': 2, 'q': 'am', 'page': 1, 'size': 20});

    await repository.list();
    captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[1], {'page': 0, 'size': 20});
  });

  test('byId() hits /api/doctors/{id}', () async {
    when(() => dio.get<Map<String, dynamic>>(any()))
        .thenAnswer((_) async => Response(
              requestOptions: RequestOptions(),
              statusCode: 200,
              data: {
                'userId': 3,
                'email': 'doc@x.y',
                'name': 'Dr. Amal',
                'specialty': null,
                'cabinetAddress': null,
                'workingHours': null,
                'phone': null,
                'bio': null,
                'acceptanceMode': 'MANUAL',
                'slotDurationMinutes': 30,
                'ratingAverage': 0,
                'ratingCount': 0,
              },
            ));

    final doctor = await repository.byId(3);

    expect(doctor.userId, 3);
    verify(() => dio.get<Map<String, dynamic>>('/api/doctors/3')).called(1);
  });

  test('me() reads /api/doctors/me', () async {
    when(() => dio.get<Map<String, dynamic>>(any()))
        .thenAnswer((_) async => _okDoctor());

    final doctor = await repository.me();

    expect(doctor.name, 'Dr. Amal');
    expect(doctor.acceptanceMode, AcceptanceMode.auto);
    expect(doctor.slotDurationMinutes, 20);
    verify(() => dio.get<Map<String, dynamic>>('/api/doctors/me')).called(1);
  });

  test('updateMe() PUTs the full body with the enum serialized', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _okDoctor());

    await repository.updateMe(
      name: 'Dr. Amal',
      specialtyId: 1,
      cabinetAddress: '1 Rue X',
      workingHours: 'Mon-Fri',
      phone: '+212600',
      bio: 'Heart specialist',
      acceptanceMode: AcceptanceMode.auto,
      slotDurationMinutes: 20,
    );

    final captured = verify(() => dio.put<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/doctors/me');
    expect(captured[1], {
      'name': 'Dr. Amal',
      'specialtyId': 1,
      'cabinetAddress': '1 Rue X',
      'workingHours': 'Mon-Fri',
      'phone': '+212600',
      'bio': 'Heart specialist',
      'acceptanceMode': 'AUTO',
      'slotDurationMinutes': 20,
    });
  });

  test('updateMe() omits absent optional fields', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _okDoctor());

    await repository.updateMe(name: 'Solo Name');

    final captured = verify(() => dio.put<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[1], {'name': 'Solo Name'});
  });

  test('reviews() pages through /api/doctors/{id}/reviews', () async {
    when(() => dio.get<Map<String, dynamic>>(any(),
            queryParameters: any(named: 'queryParameters')))
        .thenAnswer((_) async => Response(
              requestOptions: RequestOptions(),
              statusCode: 200,
              data: {
                'content': [
                  {
                    'id': 5,
                    'appointmentId': 11,
                    'doctorId': 3,
                    'patientId': 7,
                    'patientName': 'Sami',
                    'rating': 5,
                    'comment': null,
                    'createdAt': '2026-07-01T10:00:00Z',
                    'updatedAt': null,
                  }
                ],
                'page': {
                  'size': 10,
                  'number': 0,
                  'totalElements': 1,
                  'totalPages': 1
                },
              },
            ));

    final reviews = await repository.reviews(3, size: 10);

    expect(reviews.content.single.rating, 5);
    final captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[0], '/api/doctors/3/reviews');
    expect(captured[1], {'page': 0, 'size': 10});
  });
}
