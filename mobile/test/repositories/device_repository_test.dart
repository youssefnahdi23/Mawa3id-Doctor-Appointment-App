import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/notifications/data/device_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

Response<T> _ok<T>() =>
    Response<T>(requestOptions: RequestOptions(path: ''), statusCode: 204);

void main() {
  late _MockDio dio;
  late DeviceRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = DeviceRepository(dio);
  });

  test('register() posts the token and platform', () async {
    when(() => dio.post<void>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok<void>());

    await repository.register(token: 'fcm-abc', platform: 'ANDROID');

    final captured = verify(() => dio.post<void>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/notifications/devices');
    expect(captured[1], {'token': 'fcm-abc', 'platform': 'ANDROID'});
  });

  test('unregister() deletes with the token in the body', () async {
    when(() => dio.delete<void>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok<void>());

    await repository.unregister('fcm-abc');

    final captured = verify(() => dio.delete<void>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/notifications/devices');
    expect(captured[1], {'token': 'fcm-abc'});
  });
}
