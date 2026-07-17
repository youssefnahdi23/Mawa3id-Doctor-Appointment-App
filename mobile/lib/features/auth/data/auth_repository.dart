import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'auth_models.dart';

final authRepositoryProvider =
    Provider<AuthRepository>((ref) => AuthRepository(ref.watch(dioProvider)));

class AuthRepository {
  AuthRepository(this._dio);

  final Dio _dio;

  Future<AuthResponse> login({
    required String identifier,
    required String password,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/login',
        data: {'identifier': identifier, 'password': password},
      );
      return AuthResponse.fromJson(response.data!);
    });
  }

  Future<AuthResponse> registerPatient({
    required String email,
    required String password,
    required String fullName,
    required DateTime dateOfBirth,
    String? username,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/register/patient',
        data: {
          'email': email,
          'password': password,
          'fullName': fullName,
          'dateOfBirth': formatIsoDate(dateOfBirth),
          if (username != null && username.isNotEmpty) 'username': username,
        },
      );
      return AuthResponse.fromJson(response.data!);
    });
  }

  Future<AuthResponse> registerDoctor({
    required String email,
    required String password,
    required String name,
    required int specialtyId,
    String? cabinetAddress,
    String? phone,
    String? username,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/register/doctor',
        data: {
          'email': email,
          'password': password,
          'name': name,
          'specialtyId': specialtyId,
          if (cabinetAddress != null && cabinetAddress.isNotEmpty)
            'cabinetAddress': cabinetAddress,
          if (phone != null && phone.isNotEmpty) 'phone': phone,
          if (username != null && username.isNotEmpty) 'username': username,
        },
      );
      return AuthResponse.fromJson(response.data!);
    });
  }

  Future<MeResponse> me() {
    return apiCall(() async {
      final response = await _dio.get<Map<String, dynamic>>('/api/auth/me');
      return MeResponse.fromJson(response.data!);
    });
  }

  /// Revoke a refresh session server-side (best-effort logout).
  Future<void> logout(String refreshToken) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/auth/logout',
        data: {'refreshToken': refreshToken},
      );
    });
  }

  /// Revoke every refresh session for the current user (sign out everywhere).
  Future<void> logoutAll() {
    return apiCall(() async {
      await _dio.post<void>('/api/auth/logout-all');
    });
  }

  Future<void> forgotPassword(String identifier) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/auth/password/forgot',
        data: {'identifier': identifier},
      );
    });
  }

  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/auth/password/reset',
        data: {'token': token, 'newPassword': newPassword},
      );
    });
  }

  Future<void> requestEmailVerification() {
    return apiCall(() async {
      await _dio.post<void>('/api/auth/verify/email/request');
    });
  }

  Future<void> confirmEmail(String code) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/auth/verify/email/confirm',
        data: {'code': code},
      );
    });
  }

  Future<void> requestPhoneVerification(String phone) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/auth/verify/phone/request',
        data: {'phone': phone},
      );
    });
  }

  Future<void> confirmPhone({
    required String phone,
    required String code,
  }) {
    return apiCall(() async {
      await _dio.post<void>(
        '/api/auth/verify/phone/confirm',
        data: {'phone': phone, 'code': code},
      );
    });
  }
}

/// `yyyy-MM-dd` (Java `LocalDate`).
String formatIsoDate(DateTime date) {
  String two(int n) => n.toString().padLeft(2, '0');
  return '${date.year.toString().padLeft(4, '0')}-${two(date.month)}-${two(date.day)}';
}
