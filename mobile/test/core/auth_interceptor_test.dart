import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/network/dio_client.dart';
import 'package:mawa3id/core/storage/token_storage.dart';
import 'package:mocktail/mocktail.dart';

class _MockTokenStorage extends Mock implements TokenStorage {}

class _MockDio extends Mock implements Dio {}

class _RecordingRequestHandler extends RequestInterceptorHandler {
  RequestOptions? forwarded;

  @override
  void next(RequestOptions requestOptions) {
    forwarded = requestOptions;
  }
}

class _RecordingErrorHandler extends ErrorInterceptorHandler {
  final _done = Completer<void>();
  DioException? forwarded;
  Response<dynamic>? resolved;

  Future<void> get done => _done.future;

  void _complete() {
    if (!_done.isCompleted) _done.complete();
  }

  @override
  void next(DioException err) {
    forwarded = err;
    _complete();
  }

  @override
  void resolve(Response<dynamic> response,
      [bool callFollowingResponseInterceptor = false]) {
    resolved = response;
    _complete();
  }
}

void main() {
  late _MockTokenStorage storage;
  late _MockDio refreshDio;
  late bool loggedOut;
  late AuthInterceptor interceptor;

  setUpAll(() {
    registerFallbackValue(RequestOptions(path: '/'));
    registerFallbackValue(DateTime.now());
  });

  setUp(() {
    storage = _MockTokenStorage();
    refreshDio = _MockDio();
    loggedOut = false;
    when(() => storage.cachedRefreshToken).thenReturn(null);
    interceptor = AuthInterceptor(
      tokenStorage: storage,
      onUnauthorized: () => loggedOut = true,
      refreshDio: refreshDio,
    );
  });

  DioException error401(String path) {
    final options = RequestOptions(path: path);
    return DioException(
      requestOptions: options,
      response: Response(requestOptions: options, statusCode: 401),
    );
  }

  Response<Map<String, dynamic>> refreshOk() => Response(
        requestOptions: RequestOptions(path: '/api/auth/refresh'),
        statusCode: 200,
        data: {
          'token': 'new-jwt',
          'tokenType': 'Bearer',
          'expiresInMs': 1000,
          'refreshToken': 'refresh-2',
          'userId': 1,
          'username': 'sami',
          'email': 'a@b.c',
          'role': 'PATIENT',
        },
      );

  group('onRequest', () {
    test('adds the Bearer header when a token is cached', () {
      when(() => storage.cachedToken).thenReturn('jwt-token');
      final handler = _RecordingRequestHandler();
      interceptor.onRequest(RequestOptions(path: '/api/doctors'), handler);
      expect(handler.forwarded!.headers['Authorization'], 'Bearer jwt-token');
    });

    test('leaves public auth requests anonymous even with a token', () {
      when(() => storage.cachedToken).thenReturn('jwt-token');
      for (final path in [
        '/api/auth/login',
        '/api/auth/register/patient',
        '/api/auth/refresh',
        '/api/auth/password/forgot',
      ]) {
        final handler = _RecordingRequestHandler();
        interceptor.onRequest(RequestOptions(path: path), handler);
        expect(handler.forwarded!.headers.containsKey('Authorization'), isFalse,
            reason: path);
      }
    });

    test('sends nothing when logged out', () {
      when(() => storage.cachedToken).thenReturn(null);
      final handler = _RecordingRequestHandler();
      interceptor.onRequest(RequestOptions(path: '/api/doctors'), handler);
      expect(
          handler.forwarded!.headers.containsKey('Authorization'), isFalse);
    });
  });

  group('onError', () {
    test('forces logout on 401 when there is no refresh token', () async {
      final handler = _RecordingErrorHandler();
      interceptor.onError(error401('/api/appointments/me'), handler);
      await handler.done;
      expect(loggedOut, isTrue);
      expect(handler.forwarded, isNotNull);
    });

    test('does NOT force logout on 401 from login (bad credentials)',
        () async {
      final handler = _RecordingErrorHandler();
      interceptor.onError(error401('/api/auth/login'), handler);
      await handler.done;
      expect(loggedOut, isFalse);
    });

    test('401 with a refresh token → refreshes and retries the request',
        () async {
      when(() => storage.cachedRefreshToken).thenReturn('refresh-1');
      when(() => storage.cachedToken).thenReturn('new-jwt');
      when(() => storage.updateTokens(
            token: any(named: 'token'),
            refreshToken: any(named: 'refreshToken'),
            expiresAt: any(named: 'expiresAt'),
          )).thenAnswer((_) async {});
      when(() => refreshDio.post<Map<String, dynamic>>('/api/auth/refresh',
          data: any(named: 'data'))).thenAnswer((_) async => refreshOk());
      final retried = Response<dynamic>(
          requestOptions: RequestOptions(path: '/api/data'),
          statusCode: 200,
          data: 'ok');
      when(() => refreshDio.fetch<dynamic>(any()))
          .thenAnswer((_) async => retried);

      final handler = _RecordingErrorHandler();
      interceptor.onError(error401('/api/data'), handler);
      await handler.done;

      expect(loggedOut, isFalse);
      expect(handler.resolved, same(retried));
      // The retry carries the refreshed bearer.
      final sent =
          verify(() => refreshDio.fetch<dynamic>(captureAny())).captured.single
              as RequestOptions;
      expect(sent.headers['Authorization'], 'Bearer new-jwt');
      verify(() => storage.updateTokens(
            token: 'new-jwt',
            refreshToken: 'refresh-2',
            expiresAt: any(named: 'expiresAt'),
          )).called(1);
    });

    test('401 → refresh fails → forces logout', () async {
      when(() => storage.cachedRefreshToken).thenReturn('refresh-1');
      when(() => refreshDio.post<Map<String, dynamic>>('/api/auth/refresh',
          data: any(named: 'data'))).thenThrow(DioException(
        requestOptions: RequestOptions(path: '/api/auth/refresh'),
        response: Response(
            requestOptions: RequestOptions(path: '/api/auth/refresh'),
            statusCode: 401),
      ));

      final handler = _RecordingErrorHandler();
      interceptor.onError(error401('/api/data'), handler);
      await handler.done;

      expect(loggedOut, isTrue);
      expect(handler.forwarded, isNotNull);
    });

    test('does not retry a request that already carried a retry flag',
        () async {
      when(() => storage.cachedRefreshToken).thenReturn('refresh-1');
      final options = RequestOptions(path: '/api/data')
        ..extra['auth_retried'] = true;
      final err = DioException(
        requestOptions: options,
        response: Response(requestOptions: options, statusCode: 401),
      );

      final handler = _RecordingErrorHandler();
      interceptor.onError(err, handler);
      await handler.done;

      expect(loggedOut, isTrue);
      verifyNever(() => refreshDio.post<Map<String, dynamic>>(any(),
          data: any(named: 'data')));
    });
  });
}
