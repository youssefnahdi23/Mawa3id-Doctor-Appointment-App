import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/models/page_response.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/time/wall_clock.dart';
import '../../auth/data/auth_repository.dart' show formatIsoDate;
import 'appointment_models.dart';

final appointmentRepositoryProvider = Provider<AppointmentRepository>(
    (ref) => AppointmentRepository(ref.watch(dioProvider)));

class AppointmentRepository {
  AppointmentRepository(this._dio);

  final Dio _dio;

  /// The backend rejects ranges longer than this (inclusive dates).
  static const maxSlotRangeDays = 31;

  /// Fetches bookable slots for [doctorId] between two dates (inclusive).
  /// [to] is clamped so the span never exceeds [maxSlotRangeDays].
  Future<List<SlotResponse>> slots(
    int doctorId, {
    required DateTime from,
    required DateTime to,
  }) {
    final clampedTo = clampSlotRangeEnd(from: from, to: to);
    return apiCall(() async {
      final response = await _dio.get<List<dynamic>>(
        '/api/doctors/$doctorId/slots',
        queryParameters: {
          'from': formatIsoDate(from),
          'to': formatIsoDate(clampedTo),
        },
      );
      return response.data!
          .map((item) => SlotResponse.fromJson(item as Map<String, dynamic>))
          .toList();
    });
  }

  Future<AppointmentResponse> book({
    required int doctorId,
    required DateTime startTime,
    String? reason,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/appointments',
        data: {
          'doctorId': doctorId,
          'startTime': WallClock.format(startTime),
          if (reason != null && reason.isNotEmpty) 'reason': reason,
        },
      );
      return AppointmentResponse.fromJson(response.data!);
    });
  }

  /// Patient's own appointments.
  Future<PageResponse<AppointmentResponse>> mine({
    AppointmentStatus? status,
    int page = 0,
    int size = 20,
  }) {
    return _paged('/api/appointments/me', status, page, size);
  }

  /// Doctor's queue.
  Future<PageResponse<AppointmentResponse>> queue({
    AppointmentStatus? status,
    int page = 0,
    int size = 20,
  }) {
    return _paged('/api/appointments', status, page, size);
  }

  Future<AppointmentResponse> accept(int id) => _transition(id, 'accept');
  Future<AppointmentResponse> reject(int id) => _transition(id, 'reject');
  Future<AppointmentResponse> complete(int id) => _transition(id, 'complete');
  Future<AppointmentResponse> cancel(int id) => _transition(id, 'cancel');
  Future<AppointmentResponse> noShow(int id) => _transition(id, 'no-show');

  /// Move an appointment to a new start time.
  Future<AppointmentResponse> reschedule(int id, DateTime startTime) {
    return apiCall(() async {
      final response = await _dio.put<Map<String, dynamic>>(
        '/api/appointments/$id/reschedule',
        data: {'startTime': WallClock.format(startTime)},
      );
      return AppointmentResponse.fromJson(response.data!);
    });
  }

  Future<PageResponse<AppointmentResponse>> _paged(
    String path,
    AppointmentStatus? status,
    int page,
    int size,
  ) {
    return apiCall(() async {
      final response = await _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: {
          if (status != null) 'status': statusApiName(status),
          'page': page,
          'size': size,
        },
      );
      return PageResponse.fromJson(
          response.data!, AppointmentResponse.fromJson);
    });
  }

  Future<AppointmentResponse> _transition(int id, String action) {
    return apiCall(() async {
      final response = await _dio
          .put<Map<String, dynamic>>('/api/appointments/$id/$action');
      return AppointmentResponse.fromJson(response.data!);
    });
  }
}

/// Wire name of a status (`PENDING`, `NO_SHOW`, ...), for query parameters.
String statusApiName(AppointmentStatus status) => status.wireName;

/// Clamps a slot-range end so `[from, to]` spans at most
/// [AppointmentRepository.maxSlotRangeDays] inclusive calendar days.
DateTime clampSlotRangeEnd({required DateTime from, required DateTime to}) {
  final max = DateTime(from.year, from.month,
      from.day + AppointmentRepository.maxSlotRangeDays - 1);
  return to.isAfter(max) ? max : to;
}
