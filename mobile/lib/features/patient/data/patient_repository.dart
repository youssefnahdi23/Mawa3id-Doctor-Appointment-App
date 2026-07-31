import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../../auth/data/auth_repository.dart' show formatIsoDate;
import 'patient_models.dart';

final patientRepositoryProvider = Provider<PatientRepository>(
    (ref) => PatientRepository(ref.watch(dioProvider)));

class PatientRepository {
  PatientRepository(this._dio);

  final Dio _dio;

  /// The signed-in patient's own profile.
  Future<PatientResponse> me() {
    return apiCall(() async {
      final response =
          await _dio.get<Map<String, dynamic>>('/api/patients/me');
      return PatientResponse.fromJson(response.data!);
    });
  }

  /// Update the signed-in patient's profile. `fullName` is required;
  /// `dateOfBirth` is sent only when provided.
  Future<PatientResponse> updateMe({
    required String fullName,
    DateTime? dateOfBirth,
    String? phone,
  }) {
    return apiCall(() async {
      final response = await _dio.put<Map<String, dynamic>>(
        '/api/patients/me',
        data: {
          'fullName': fullName,
          if (dateOfBirth != null) 'dateOfBirth': formatIsoDate(dateOfBirth),
          if (phone != null) 'phone': phone,
        },
      );
      return PatientResponse.fromJson(response.data!);
    });
  }
}
