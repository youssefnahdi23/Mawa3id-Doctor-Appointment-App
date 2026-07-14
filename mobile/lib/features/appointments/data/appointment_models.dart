import 'package:json_annotation/json_annotation.dart';

import '../../../core/time/wall_clock.dart';

part 'appointment_models.g.dart';

enum AppointmentStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('ACCEPTED')
  accepted,
  @JsonValue('REJECTED')
  rejected,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('COMPLETED')
  completed,
}

@JsonSerializable(createToJson: false)
class AppointmentResponse {
  const AppointmentResponse({
    required this.id,
    required this.doctorId,
    required this.doctorName,
    required this.patientId,
    required this.patientName,
    required this.start,
    required this.end,
    required this.status,
    this.reason,
  });

  factory AppointmentResponse.fromJson(Map<String, dynamic> json) =>
      _$AppointmentResponseFromJson(json);

  final int id;
  final int doctorId;
  final String doctorName;
  final int patientId;
  final String patientName;

  /// Clinic wall-clock time — display as-is, never zone-convert.
  @WallClockConverter()
  final DateTime start;

  @WallClockConverter()
  final DateTime end;

  final AppointmentStatus status;
  final String? reason;
}

/// A bookable time slot: start and (exclusive) end, clinic wall-clock.
@JsonSerializable(createToJson: false)
class SlotResponse {
  const SlotResponse({required this.start, required this.end});

  factory SlotResponse.fromJson(Map<String, dynamic> json) =>
      _$SlotResponseFromJson(json);

  @WallClockConverter()
  final DateTime start;

  @WallClockConverter()
  final DateTime end;
}
