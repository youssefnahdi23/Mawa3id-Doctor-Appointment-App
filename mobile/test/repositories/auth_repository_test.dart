import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

Response<Map<String, dynamic>> _ok(Map<String, dynamic> data) =>
    Response(requestOptions: RequestOptions(), data: data, statusCode: 200);

Response<void> _okVoid() =>
    Response(requestOptions: RequestOptions(), statusCode: 200);

final _authJson = {
  'token': 'jwt',
  'tokenType': 'Bearer',
  'expiresInMs': 1000,
  'refreshToken': 'refresh-1',
  'userId': 1,
  'username': 'sami',
  'email': 'patient@example.com',
  'role': 'PATIENT',
};

void main() {
  late _MockDio dio;
  late AuthRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = AuthRepository(dio);
  });

  void stubPost() {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_authJson));
  }

  void stubVoidPost() {
    when(() => dio.post<void>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _okVoid());
    when(() => dio.post<void>(any())).thenAnswer((_) async => _okVoid());
  }

  test('login posts identifier to /api/auth/login and parses tokens', () async {
    stubPost();

    final auth =
        await repository.login(identifier: 'sami', password: 'password123');

    expect(auth.token, 'jwt');
    expect(auth.refreshToken, 'refresh-1');
    expect(auth.username, 'sami');
    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/login');
    expect(captured[1], {'identifier': 'sami', 'password': 'password123'});
  });

  test('registerPatient serializes dateOfBirth as yyyy-MM-dd', () async {
    stubPost();

    await repository.registerPatient(
      email: 'patient@example.com',
      password: 'password123',
      fullName: 'Sami Ben',
      dateOfBirth: DateTime(1995, 3, 7),
    );

    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/register/patient');
    expect((captured[1] as Map)['dateOfBirth'], '1995-03-07');
  });

  test('registerPatient includes username when provided', () async {
    stubPost();

    await repository.registerPatient(
      email: 'patient@example.com',
      password: 'password123',
      fullName: 'Sami Ben',
      dateOfBirth: DateTime(1995, 3, 7),
      username: 'sami.b',
    );

    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect((captured[1] as Map)['username'], 'sami.b');
  });

  test('registerPatient omits an empty username', () async {
    stubPost();

    await repository.registerPatient(
      email: 'patient@example.com',
      password: 'password123',
      fullName: 'Sami Ben',
      dateOfBirth: DateTime(1995, 3, 7),
      username: '',
    );

    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect((captured[1] as Map).containsKey('username'), isFalse);
  });

  test('registerDoctor omits empty optional fields', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok({..._authJson, 'role': 'DOCTOR'}));

    final auth = await repository.registerDoctor(
      email: 'doctor@example.com',
      password: 'password123',
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

  test('me() reads /api/auth/me including username and verification', () async {
    when(() => dio.get<Map<String, dynamic>>(any())).thenAnswer((_) async =>
        _ok({
          'userId': 1,
          'username': 'sami',
          'email': 'patient@example.com',
          'role': 'PATIENT',
          'emailVerified': true,
          'phoneVerified': false,
        }));

    final me = await repository.me();

    expect(me.role, UserRole.patient);
    expect(me.username, 'sami');
    expect(me.emailVerified, isTrue);
    expect(me.phoneVerified, isFalse);
    verify(() => dio.get<Map<String, dynamic>>('/api/auth/me')).called(1);
  });

  test('logoutAll posts to /api/auth/logout-all with no body', () async {
    stubVoidPost();

    await repository.logoutAll();

    verify(() => dio.post<void>('/api/auth/logout-all')).called(1);
  });

  test('forgotPassword posts the identifier', () async {
    stubVoidPost();

    await repository.forgotPassword('sami@x.y');

    final captured = verify(() => dio.post<void>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/password/forgot');
    expect(captured[1], {'identifier': 'sami@x.y'});
  });

  test('resetPassword posts token and new password', () async {
    stubVoidPost();

    await repository.resetPassword(token: 'code-9', newPassword: 'newpassword123');

    final captured = verify(() => dio.post<void>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/password/reset');
    expect(captured[1], {'token': 'code-9', 'newPassword': 'newpassword123'});
  });

  test('confirmPhone posts phone and code', () async {
    stubVoidPost();

    await repository.confirmPhone(phone: '+212600', code: '123456');

    final captured = verify(() => dio.post<void>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/auth/verify/phone/confirm');
    expect(captured[1], {'phone': '+212600', 'code': '123456'});
  });
}
