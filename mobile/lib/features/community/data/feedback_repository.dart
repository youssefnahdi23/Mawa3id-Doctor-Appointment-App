import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'feedback_models.dart';

final feedbackRepositoryProvider = Provider<FeedbackRepository>(
    (ref) => FeedbackRepository(ref.watch(dioProvider)));

class FeedbackRepository {
  FeedbackRepository(this._dio);

  final Dio _dio;

  /// Submits app feedback for the signed-in user, tagging it with the app
  /// version and platform so maintainers can triage it.
  Future<void> submit({
    required FeedbackCategory category,
    required String message,
  }) {
    return apiCall(() async {
      await _dio.post<Map<String, dynamic>>(
        '/api/feedback',
        data: {
          'category': category.wireName,
          'message': message,
          'appVersion': AppConfig.appVersion,
          'platform': _platform(),
        },
      );
    });
  }

  String _platform() {
    if (kIsWeb) return 'WEB';
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return 'ANDROID';
      case TargetPlatform.iOS:
        return 'IOS';
      default:
        return defaultTargetPlatform.name.toUpperCase();
    }
  }
}
