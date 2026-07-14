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
    required String email,
    required String password,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/login',
        data: {'email': email, 'password': password},
      );
      return AuthResponse.fromJson(response.data!);
    });
  }

  Future<AuthResponse> registerPatient({
    required String email,
    required String password,
    required String fullName,
    required DateTime dateOfBirth,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/register/patient',
        data: {
          'email': email,
          'password': password,
          'fullName': fullName,
          'dateOfBirth': formatIsoDate(dateOfBirth),
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
}

/// `yyyy-MM-dd` (Java `LocalDate`).
String formatIsoDate(DateTime date) {
  String two(int n) => n.toString().padLeft(2, '0');
  return '${date.year.toString().padLeft(4, '0')}-${two(date.month)}-${two(date.day)}';
}
