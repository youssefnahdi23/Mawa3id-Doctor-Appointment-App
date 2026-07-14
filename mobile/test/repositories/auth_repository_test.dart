import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

Response<Map<String, dynamic>> _ok(Map<String, dynamic> data) =>
    Response(requestOptions: RequestOptions(), data: data, statusCode: 200);

final _authJson = {
  'token': 'jwt',
  'tokenType': 'Bearer',
  'expiresInMs': 1000,
  'userId': 1,
  'email': 'a@b.c',
  'role': 'PATIENT',
};

void main() {
  late _MockDio dio;
  late AuthRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = AuthRepository(dio);
  });

  test('login posts credentials to /api/auth/login', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_authJson));

    final auth = await repository.login(email: 'a@b.c', password: 'secret12');

    expect(auth.token, 'jwt');
    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/login');
    expect(captured[1], {'email': 'a@b.c', 'password': 'secret12'});
  });

  test('registerPatient serializes dateOfBirth as yyyy-MM-dd', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_authJson));

    await repository.registerPatient(
      email: 'a@b.c',
      password: 'secret12',
      fullName: 'Sami Ben',
      dateOfBirth: DateTime(1995, 3, 7),
    );

    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/register/patient');
    expect((captured[1] as Map)['dateOfBirth'], '1995-03-07');
  });

  test('registerDoctor omits empty optional fields', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer(
            (_) async => _ok({..._authJson, 'role': 'DOCTOR'}));

    final auth = await repository.registerDoctor(
      email: 'doc@x.y',
      password: 'secret12',
      name: 'Dr. Amal',
      specialtyId: 2,
      cabinetAddress: '',
      phone: '',
    );

    expect(auth.role, UserRole.doctor);
    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/register/doctor');
    final body = captured[1] as Map;
    expect(body['specialtyId'], 2);
    expect(body.containsKey('cabinetAddress'), isFalse);
    expect(body.containsKey('phone'), isFalse);
  });

  test('me() reads /api/auth/me', () async {
    when(() => dio.get<Map<String, dynamic>>(any())).thenAnswer(
        (_) async => _ok({'userId': 1, 'email': 'a@b.c', 'role': 'PATIENT'}));

    final me = await repository.me();

    expect(me.role, UserRole.patient);
    verify(() => dio.get<Map<String, dynamic>>('/api/auth/me')).called(1);
  });
}
