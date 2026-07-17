import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/patient/data/patient_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

final _patientJson = {
  'userId': 7,
  'email': 'p@x.y',
  'fullName': 'Sami Ben',
  'dateOfBirth': '1995-03-07',
  'patientCode': 'MW-ABC234',
};

Response<Map<String, dynamic>> _okPatient() => Response(
    requestOptions: RequestOptions(), statusCode: 200, data: _patientJson);

void main() {
  late _MockDio dio;
  late PatientRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = PatientRepository(dio);
  });

  test('me() reads /api/patients/me', () async {
    when(() => dio.get<Map<String, dynamic>>(any()))
        .thenAnswer((_) async => _okPatient());

    final patient = await repository.me();

    expect(patient.fullName, 'Sami Ben');
    expect(patient.patientCode, 'MW-ABC234');
    expect(patient.dateOfBirth, DateTime(1995, 3, 7));
    verify(() => dio.get<Map<String, dynamic>>('/api/patients/me')).called(1);
  });

  test('updateMe() PUTs fullName and yyyy-MM-dd dateOfBirth', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _okPatient());

    await repository.updateMe(
      fullName: 'Sami Ben',
      dateOfBirth: DateTime(1995, 3, 7),
    );

    final captured = verify(() => dio.put<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/patients/me');
    expect(captured[1], {'fullName': 'Sami Ben', 'dateOfBirth': '1995-03-07'});
  });

  test('updateMe() omits an absent dateOfBirth', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _okPatient());

    await repository.updateMe(fullName: 'Sami Ben');

    final captured = verify(() => dio.put<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[1], {'fullName': 'Sami Ben'});
  });
}
