import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/network/dio_client.dart';
import 'package:mawa3id/core/storage/token_storage.dart';
import 'package:mocktail/mocktail.dart';

class _MockTokenStorage extends Mock implements TokenStorage {}

class _RecordingRequestHandler extends RequestInterceptorHandler {
  RequestOptions? forwarded;

  @override
  void next(RequestOptions requestOptions) {
    forwarded = requestOptions;
  }
}

class _RecordingErrorHandler extends ErrorInterceptorHandler {
  DioException? forwarded;

  @override
  void next(DioException err) {
    forwarded = err;
  }
}

void main() {
  late _MockTokenStorage storage;
  late bool loggedOut;
  late AuthInterceptor interceptor;

  setUp(() {
    storage = _MockTokenStorage();
    loggedOut = false;
    interceptor = AuthInterceptor(
      tokenStorage: storage,
      onUnauthorized: () => loggedOut = true,
    );
  });

  DioException error401(String path) {
    final options = RequestOptions(path: path);
    return DioException(
      requestOptions: options,
      response: Response(requestOptions: options, statusCode: 401),
    );
  }

  group('onRequest', () {
    test('adds the Bearer header when a token is cached', () {
      when(() => storage.cachedToken).thenReturn('jwt-token');
      final handler = _RecordingRequestHandler();
      interceptor.onRequest(RequestOptions(path: '/api/doctors'), handler);
      expect(handler.forwarded!.headers['Authorization'], 'Bearer jwt-token');
    });

    test('leaves login/register requests anonymous even with a token', () {
      when(() => storage.cachedToken).thenReturn('jwt-token');
      for (final path in [
        '/api/auth/login',
        '/api/auth/register/patient',
        '/api/auth/register/doctor',
      ]) {
        final handler = _RecordingRequestHandler();
        interceptor.onRequest(RequestOptions(path: path), handler);
        expect(handler.forwarded!.headers.containsKey('Authorization'),
            isFalse,
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
    test('forces logout on 401 from an authenticated endpoint', () {
      interceptor.onError(
          error401('/api/appointments/me'), _RecordingErrorHandler());
      expect(loggedOut, isTrue);
    });

    test('does NOT force logout on 401 from login (bad credentials)', () {
      interceptor.onError(
          error401('/api/auth/login'), _RecordingErrorHandler());
      expect(loggedOut, isFalse);
    });

    test('forwards the error either way', () {
      final handler = _RecordingErrorHandler();
      final err = error401('/api/appointments/me');
      interceptor.onError(err, handler);
      expect(handler.forwarded, same(err));
    });
  });
}
