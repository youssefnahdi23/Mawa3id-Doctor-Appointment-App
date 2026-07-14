import 'package:dio/dio.dart';

/// Broad classification of an API failure, used by the UI to pick a
/// localized message.
enum ApiErrorKind {
  /// 400 — validation failed; [ApiException.fieldErrors] may name the fields.
  badRequest,

  /// 401 — bad credentials or expired/invalid token.
  unauthorized,

  /// 403 — authenticated but not allowed.
  forbidden,

  /// 404.
  notFound,

  /// 409 — illegal state transition or slot already taken.
  conflict,

  /// 429 — rate limited (auth endpoints allow 10 requests / 60s / IP).
  rateLimited,

  /// 5xx.
  server,

  /// No response at all (offline, DNS, timeout).
  network,

  /// Anything else.
  unknown,
}

/// Normalized API error carrying the backend's `ApiError` payload
/// ({status, message, fieldErrors?}) when one was returned.
class ApiException implements Exception {
  const ApiException({
    required this.kind,
    this.statusCode,
    this.message,
    this.fieldErrors = const {},
  });

  factory ApiException.fromDio(DioException e) {
    final response = e.response;
    if (response == null) {
      return ApiException(kind: ApiErrorKind.network, message: e.message);
    }
    String? message;
    var fieldErrors = const <String, String>{};
    final data = response.data;
    if (data is Map) {
      message = data['message'] as String?;
      final rawFieldErrors = data['fieldErrors'];
      if (rawFieldErrors is Map) {
        fieldErrors = rawFieldErrors
            .map((key, value) => MapEntry(key.toString(), value.toString()));
      }
    }
    final status = response.statusCode ?? 0;
    final kind = switch (status) {
      400 => ApiErrorKind.badRequest,
      401 => ApiErrorKind.unauthorized,
      403 => ApiErrorKind.forbidden,
      404 => ApiErrorKind.notFound,
      409 => ApiErrorKind.conflict,
      429 => ApiErrorKind.rateLimited,
      >= 500 => ApiErrorKind.server,
      _ => ApiErrorKind.unknown,
    };
    return ApiException(
      kind: kind,
      statusCode: status,
      message: message,
      fieldErrors: fieldErrors,
    );
  }

  final ApiErrorKind kind;
  final int? statusCode;

  /// Server-provided human-readable message (English); prefer localized
  /// text keyed off [kind] and fall back to this.
  final String? message;

  /// Bean-validation errors keyed by request field name.
  final Map<String, String> fieldErrors;

  @override
  String toString() =>
      'ApiException($kind, status: $statusCode, message: $message)';
}

/// Runs [request] and rethrows any [DioException] as an [ApiException].
/// Every repository method goes through this.
Future<T> apiCall<T>(Future<T> Function() request) async {
  try {
    return await request();
  } on DioException catch (e) {
    throw ApiException.fromDio(e);
  }
}
