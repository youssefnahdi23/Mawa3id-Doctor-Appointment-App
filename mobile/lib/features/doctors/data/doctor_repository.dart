import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/models/page_response.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'doctor_models.dart';

final doctorRepositoryProvider = Provider<DoctorRepository>(
    (ref) => DoctorRepository(ref.watch(dioProvider)));

class DoctorRepository {
  DoctorRepository(this._dio);

  final Dio _dio;

  Future<List<SpecialtyResponse>> specialties() {
    return apiCall(() async {
      final response = await _dio.get<List<dynamic>>('/api/specialties');
      return response.data!
          .map((item) =>
              SpecialtyResponse.fromJson(item as Map<String, dynamic>))
          .toList();
    });
  }

  Future<PageResponse<DoctorSummary>> list({
    int? specialtyId,
    String? query,
    bool? cnam,
    String? governorate,
    int page = 0,
    int size = 20,
  }) {
    return apiCall(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/doctors',
        queryParameters: {
          if (specialtyId != null) 'specialtyId': specialtyId,
          if (query != null && query.isNotEmpty) 'q': query,
          if (cnam != null) 'cnam': cnam,
          if (governorate != null) 'governorate': governorate,
          'page': page,
          'size': size,
        },
      );
      return PageResponse.fromJson(response.data!, DoctorSummary.fromJson);
    });
  }

  Future<DoctorResponse> byId(int id) {
    return apiCall(() async {
      final response =
          await _dio.get<Map<String, dynamic>>('/api/doctors/$id');
      return DoctorResponse.fromJson(response.data!);
    });
  }

  /// The signed-in doctor's own profile.
  Future<DoctorResponse> me() {
    return apiCall(() async {
      final response =
          await _dio.get<Map<String, dynamic>>('/api/doctors/me');
      return DoctorResponse.fromJson(response.data!);
    });
  }

  /// Update the signed-in doctor's profile. `name` is required; the rest are
  /// sent only when provided so blanks don't clobber unless intended.
  Future<DoctorResponse> updateMe({
    required String name,
    int? specialtyId,
    String? cabinetAddress,
    String? workingHours,
    String? phone,
    String? bio,
    AcceptanceMode? acceptanceMode,
    int? slotDurationMinutes,
    bool? cnamConventionne,
    String? governorate,
    int? consultationFee,
    List<String>? languages,
  }) {
    return apiCall(() async {
      final response = await _dio.put<Map<String, dynamic>>(
        '/api/doctors/me',
        data: {
          'name': name,
          if (specialtyId != null) 'specialtyId': specialtyId,
          if (cabinetAddress != null) 'cabinetAddress': cabinetAddress,
          if (workingHours != null) 'workingHours': workingHours,
          if (phone != null) 'phone': phone,
          if (bio != null) 'bio': bio,
          if (acceptanceMode != null)
            'acceptanceMode': acceptanceMode.wireName,
          if (slotDurationMinutes != null)
            'slotDurationMinutes': slotDurationMinutes,
          if (cnamConventionne != null) 'cnamConventionne': cnamConventionne,
          // governorate/consultationFee are absolute on the backend: omitting a
          // null clears it, which is what "unset in the form" means here.
          if (governorate != null) 'governorate': governorate,
          if (consultationFee != null) 'consultationFee': consultationFee,
          if (languages != null) 'languages': languages,
        },
      );
      return DoctorResponse.fromJson(response.data!);
    });
  }

  Future<PageResponse<ReviewResponse>> reviews(
    int doctorId, {
    int page = 0,
    int size = 20,
  }) {
    return apiCall(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/doctors/$doctorId/reviews',
        queryParameters: {'page': page, 'size': size},
      );
      return PageResponse.fromJson(response.data!, ReviewResponse.fromJson);
    });
  }
}
