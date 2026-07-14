import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/models/page_response.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'notification_models.dart';

final notificationRepositoryProvider = Provider<NotificationRepository>(
    (ref) => NotificationRepository(ref.watch(dioProvider)));

class NotificationRepository {
  NotificationRepository(this._dio);

  final Dio _dio;

  Future<PageResponse<NotificationResponse>> list({
    bool unreadOnly = false,
    int page = 0,
    int size = 20,
  }) {
    return apiCall(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        '/api/notifications',
        queryParameters: {
          if (unreadOnly) 'unread': true,
          'page': page,
          'size': size,
        },
      );
      return PageResponse.fromJson(
          response.data!, NotificationResponse.fromJson);
    });
  }

  Future<int> unreadCount() {
    return apiCall(() async {
      final response = await _dio
          .get<Map<String, dynamic>>('/api/notifications/unread-count');
      return (response.data!['count'] as num).toInt();
    });
  }

  Future<NotificationResponse> markRead(int id) {
    return apiCall(() async {
      final response = await _dio
          .put<Map<String, dynamic>>('/api/notifications/$id/read');
      return NotificationResponse.fromJson(response.data!);
    });
  }

  /// Returns how many notifications were newly marked read.
  Future<int> markAllRead() {
    return apiCall(() async {
      final response =
          await _dio.put<Map<String, dynamic>>('/api/notifications/read-all');
      return (response.data!['updated'] as num).toInt();
    });
  }
}
