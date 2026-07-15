import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/models/page_response.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'record_models.dart';

final recordRepositoryProvider = Provider<RecordRepository>(
    (ref) => RecordRepository(ref.watch(dioProvider)));

class RecordRepository {
  RecordRepository(this._dio);

  final Dio _dio;

  /// Creates the record for [appointmentId] (doctor only). Attaching a record
  /// to an ACCEPTED appointment auto-completes it. Throws a [conflict]
  /// [ApiException] if a record already exists.
  Future<MedicalRecordResponse> create(int appointmentId, RecordRequest body) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/appointments/$appointmentId/record',
        data: body.toJson(),
      );
      return MedicalRecordResponse.fromJson(response.data!);
    });
  }

  Future<MedicalRecordResponse> update(int appointmentId, RecordRequest body) {
    return apiCall(() async {
      final response = await _dio.put<Map<String, dynamic>>(
        '/api/appointments/$appointmentId/record',
        data: body.toJson(),
      );
      return MedicalRecordResponse.fromJson(response.data!);
    });
  }

  /// The record for [appointmentId], or `null` if none exists yet (404).
  Future<MedicalRecordResponse?> get(int appointmentId) {
    return apiCall(() async {
      try {
        final response = await _dio.get<Map<String, dynamic>>(
            '/api/appointments/$appointmentId/record');
        return MedicalRecordResponse.fromJson(response.data!);
      } on DioException catch (e) {
        if (e.response?.statusCode == 404) return null;
        rethrow;
      }
    });
  }

  /// The signed-in patient's record history, newest visit first.
  Future<PageResponse<MedicalRecordResponse>> mine({
    int page = 0,
    int size = 20,
  }) {
    return apiCall(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/records/me',
        queryParameters: {'page': page, 'size': size},
      );
      return PageResponse.fromJson(
          response.data!, MedicalRecordResponse.fromJson);
    });
  }
}
