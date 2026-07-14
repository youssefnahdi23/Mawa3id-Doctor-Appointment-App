import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';
import '../storage/token_storage.dart';
import 'auth_events.dart';

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: AppConfig.apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 20),
    ),
  );
  dio.interceptors.add(
    AuthInterceptor(
      tokenStorage: ref.watch(tokenStorageProvider),
      onUnauthorized: ref.watch(authEventsProvider).forceLogout,
    ),
  );
  return dio;
});

/// Injects `Authorization: Bearer` on every call except login/register, and
/// forces a logout when a token the backend previously accepted comes back
/// 401 (revoked or expired server-side).
class AuthInterceptor extends Interceptor {
  AuthInterceptor({required this.tokenStorage, required this.onUnauthorized});

  final TokenStorage tokenStorage;
  final void Function() onUnauthorized;

  static const _anonymousPaths = ['/api/auth/login', '/api/auth/register'];

  static bool _isAnonymous(String path) =>
      _anonymousPaths.any(path.startsWith);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = tokenStorage.cachedToken;
    if (token != null && !_isAnonymous(options.path)) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401 &&
        !_isAnonymous(err.requestOptions.path)) {
      onUnauthorized();
    }
    handler.next(err);
  }
}
