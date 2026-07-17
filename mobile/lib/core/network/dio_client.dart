import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';
import '../storage/token_storage.dart';
import '../../features/auth/data/auth_models.dart';
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

/// Injects `Authorization: Bearer` on every call except the public auth
/// endpoints, and on a 401 transparently refreshes the access token and retries
/// the request once. If the refresh itself fails, it forces a logout.
class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required this.tokenStorage,
    required this.onUnauthorized,
    Dio? refreshDio,
  }) : _refreshDio = refreshDio ??
            Dio(
              BaseOptions(
                baseUrl: AppConfig.apiBaseUrl,
                connectTimeout: const Duration(seconds: 10),
                receiveTimeout: const Duration(seconds: 20),
              ),
            );

  final TokenStorage tokenStorage;
  final void Function() onUnauthorized;

  /// A bare client with no interceptor, used both to refresh the token and to
  /// replay the original request — so neither can recurse back into this filter.
  final Dio _refreshDio;

  /// Shared across concurrent 401s so a burst triggers a single refresh.
  Future<bool>? _refreshing;

  static const _retriedKey = 'auth_retried';

  /// Public auth flows that must not carry a bearer and must not trigger a
  /// refresh-and-retry on 401. Verification endpoints are intentionally absent —
  /// they require auth, so their 401s should refresh.
  static const _anonymousPaths = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/logout',
    '/api/auth/password/forgot',
    '/api/auth/password/reset',
  ];

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
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final options = err.requestOptions;
    final isRetryable = err.response?.statusCode == 401 &&
        !_isAnonymous(options.path) &&
        options.extra[_retriedKey] != true;

    if (!isRetryable) {
      // A non-refreshable 401 (already retried, or a public path) still means the
      // session is gone.
      if (err.response?.statusCode == 401 && !_isAnonymous(options.path)) {
        onUnauthorized();
      }
      return handler.next(err);
    }

    final refreshToken = tokenStorage.cachedRefreshToken;
    if (refreshToken == null) {
      onUnauthorized();
      return handler.next(err);
    }

    final refreshed = await _ensureRefreshed(refreshToken);
    if (!refreshed) {
      onUnauthorized();
      return handler.next(err);
    }

    try {
      options.extra[_retriedKey] = true;
      options.headers['Authorization'] = 'Bearer ${tokenStorage.cachedToken}';
      final response = await _refreshDio.fetch<dynamic>(options);
      return handler.resolve(response);
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) {
        onUnauthorized();
      }
      return handler.next(e);
    }
  }

  /// Refresh the access token, collapsing concurrent callers onto one request.
  Future<bool> _ensureRefreshed(String refreshToken) {
    return _refreshing ??=
        _doRefresh(refreshToken).whenComplete(() => _refreshing = null);
  }

  Future<bool> _doRefresh(String refreshToken) async {
    try {
      final response = await _refreshDio.post<Map<String, dynamic>>(
        '/api/auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      final auth = AuthResponse.fromJson(response.data!);
      await tokenStorage.updateTokens(
        token: auth.token,
        refreshToken: auth.refreshToken,
        expiresAt: DateTime.now().add(Duration(milliseconds: auth.expiresInMs)),
      );
      return true;
    } on DioException {
      return false;
    }
  }
}
