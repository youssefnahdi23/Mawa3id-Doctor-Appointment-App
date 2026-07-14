import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/network/api_exception.dart';

DioException _dioError(int status, {Object? data}) {
  final options = RequestOptions(path: '/api/test');
  return DioException(
    requestOptions: options,
    response: Response(
      requestOptions: options,
      statusCode: status,
      data: data,
    ),
  );
}

void main() {
  group('ApiException.fromDio', () {
    test('maps 400 with fieldErrors from the ApiError payload', () {
      final exception = ApiException.fromDio(_dioError(400, data: {
        'status': 400,
        'message': 'Validation failed',
        'fieldErrors': {'email': 'must be a well-formed email address'},
      }));
      expect(exception.kind, ApiErrorKind.badRequest);
      expect(exception.statusCode, 400);
      expect(exception.message, 'Validation failed');
      expect(exception.fieldErrors,
          {'email': 'must be a well-formed email address'});
    });

    test('maps 401 / 403 / 404 / 409 / 429 to their kinds', () {
      expect(ApiException.fromDio(_dioError(401)).kind,
          ApiErrorKind.unauthorized);
      expect(
          ApiException.fromDio(_dioError(403)).kind, ApiErrorKind.forbidden);
      expect(
          ApiException.fromDio(_dioError(404)).kind, ApiErrorKind.notFound);
      expect(
          ApiException.fromDio(_dioError(409)).kind, ApiErrorKind.conflict);
      expect(ApiException.fromDio(_dioError(429)).kind,
          ApiErrorKind.rateLimited);
    });

    test('maps 5xx to server', () {
      expect(ApiException.fromDio(_dioError(500)).kind, ApiErrorKind.server);
      expect(ApiException.fromDio(_dioError(503)).kind, ApiErrorKind.server);
    });

    test('maps a response-less error (offline/timeout) to network', () {
      final exception = ApiException.fromDio(DioException(
        requestOptions: RequestOptions(path: '/api/test'),
        type: DioExceptionType.connectionTimeout,
      ));
      expect(exception.kind, ApiErrorKind.network);
      expect(exception.statusCode, isNull);
    });

    test('tolerates a non-JSON error body', () {
      final exception =
          ApiException.fromDio(_dioError(409, data: 'plain text'));
      expect(exception.kind, ApiErrorKind.conflict);
      expect(exception.message, isNull);
      expect(exception.fieldErrors, isEmpty);
    });
  });

  group('apiCall', () {
    test('rethrows DioException as ApiException', () async {
      await expectLater(
        apiCall(() async => throw _dioError(409)),
        throwsA(isA<ApiException>()
            .having((e) => e.kind, 'kind', ApiErrorKind.conflict)),
      );
    });

    test('passes through the successful value', () async {
      expect(await apiCall(() async => 42), 42);
    });
  });
}
