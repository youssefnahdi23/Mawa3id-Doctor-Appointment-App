import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';

final deviceRepositoryProvider =
    Provider<DeviceRepository>((ref) => DeviceRepository(ref.watch(dioProvider)));

/// Registers this device's push (FCM) token with the backend so the server can
/// deliver notifications when the app is closed. JWT auth is attached
/// automatically by the shared [dioProvider] interceptor.
class DeviceRepository {
  DeviceRepository(this._dio);

  final Dio _dio;

  /// Upsert the token for the signed-in user. [platform] is `ANDROID` or `IOS`.
  Future<void> register({required String token, required String platform}) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/notifications/devices',
        data: {'token': token, 'platform': platform},
      );
    });
  }

  /// Remove the token (on logout). Idempotent server-side.
  Future<void> unregister(String token) {
    return apiCall(() async {
      await _dio.delete<void>(
        '/api/notifications/devices',
        data: {'token': token},
      );
    });
  }
}
