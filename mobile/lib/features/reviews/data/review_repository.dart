import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../../doctors/data/doctor_models.dart' show ReviewResponse;

final reviewRepositoryProvider = Provider<ReviewRepository>(
    (ref) => ReviewRepository(ref.watch(dioProvider)));

/// Payload for creating or updating a review: a 1–5 star rating and an
/// optional comment (empty comments are omitted).
class ReviewRequest {
  const ReviewRequest({required this.rating, this.comment});

  final int rating;
  final String? comment;

  Map<String, dynamic> toJson() => {
        'rating': rating,
        if (comment != null && comment!.isNotEmpty) 'comment': comment,
      };
}

class ReviewRepository {
  ReviewRepository(this._dio);

  final Dio _dio;

  /// Creates the patient's review for [appointmentId] (only after a COMPLETED
  /// visit). Throws a [conflict] [ApiException] if a review already exists.
  Future<ReviewResponse> create(int appointmentId, ReviewRequest body) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/appointments/$appointmentId/review',
        data: body.toJson(),
      );
      return ReviewResponse.fromJson(response.data!);
    });
  }

  Future<ReviewResponse> update(int appointmentId, ReviewRequest body) {
    return apiCall(() async {
      final response = await _dio.put<Map<String, dynamic>>(
        '/api/appointments/$appointmentId/review',
        data: body.toJson(),
      );
      return ReviewResponse.fromJson(response.data!);
    });
  }

  /// The review for [appointmentId], or `null` if the patient hasn't left one
  /// yet (404).
  Future<ReviewResponse?> get(int appointmentId) {
    return apiCall(() async {
      try {
        final response = await _dio.get<Map<String, dynamic>>(
            '/api/appointments/$appointmentId/review');
        return ReviewResponse.fromJson(response.data!);
      } on DioException catch (e) {
        if (e.response?.statusCode == 404) return null;
        rethrow;
      }
    });
  }
}
