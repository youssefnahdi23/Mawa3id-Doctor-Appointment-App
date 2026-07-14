import 'package:json_annotation/json_annotation.dart';

part 'notification_models.g.dart';

enum NotificationType {
  @JsonValue('APPOINTMENT_BOOKED')
  appointmentBooked,
  @JsonValue('APPOINTMENT_ACCEPTED')
  appointmentAccepted,
  @JsonValue('APPOINTMENT_REJECTED')
  appointmentRejected,
  @JsonValue('APPOINTMENT_CANCELLED')
  appointmentCancelled,
  @JsonValue('APPOINTMENT_COMPLETED')
  appointmentCompleted,
  @JsonValue('APPOINTMENT_REMINDER')
  appointmentReminder,

  /// Forward compatibility: types added by future backend versions.
  unknown,
}

@JsonSerializable(createToJson: false)
class NotificationResponse {
  const NotificationResponse({
    required this.id,
    required this.type,
    required this.message,
    this.appointmentId,
    required this.read,
    required this.createdAt,
  });

  factory NotificationResponse.fromJson(Map<String, dynamic> json) =>
      _$NotificationResponseFromJson(json);

  final int id;

  @JsonKey(unknownEnumValue: NotificationType.unknown)
  final NotificationType type;

  final String message;
  final int? appointmentId;
  final bool read;

  /// UTC instant — the one API timestamp that IS zoned; convert to local
  /// for relative-time display.
  final DateTime createdAt;
}
