import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'availability_models.dart';

final availabilityRepositoryProvider = Provider<AvailabilityRepository>(
    (ref) => AvailabilityRepository(ref.watch(dioProvider)));

class AvailabilityRepository {
  AvailabilityRepository(this._dio);

  final Dio _dio;

  Future<List<AvailabilityRule>> forDoctor(int doctorId) {
    return apiCall(() async {
      final response =
          await _dio.get<List<dynamic>>('/api/doctors/$doctorId/availability');
      return response.data!
          .map(
              (item) => AvailabilityRule.fromJson(item as Map<String, dynamic>))
          .toList();
    });
  }

  /// Full replacement of the signed-in doctor's weekly rules; an empty list
  /// clears the schedule.
  Future<List<AvailabilityRule>> updateMine(List<AvailabilityRule> rules) {
    return apiCall(() async {
      final response = await _dio.put<List<dynamic>>(
        '/api/doctors/me/availability',
        data: {'rules': rules.map((rule) => rule.toJson()).toList()},
      );
      return response.data!
          .map(
              (item) => AvailabilityRule.fromJson(item as Map<String, dynamic>))
          .toList();
    });
  }
}
