import 'package:json_annotation/json_annotation.dart';

part 'doctor_models.g.dart';

/// How a doctor handles booking requests: MANUAL leaves them PENDING until
/// accepted, AUTO confirms immediately.
enum AcceptanceMode {
  @JsonValue('MANUAL')
  manual,
  @JsonValue('AUTO')
  auto,
}

@JsonSerializable(createToJson: false)
class SpecialtyResponse {
  const SpecialtyResponse({
    required this.id,
    required this.name,
    this.description,
  });

  factory SpecialtyResponse.fromJson(Map<String, dynamic> json) =>
      _$SpecialtyResponseFromJson(json);

  final int id;
  final String name;
  final String? description;
}

/// Lightweight projection used by the doctor list/search endpoint.
@JsonSerializable(createToJson: false)
class DoctorSummary {
  const DoctorSummary({
    required this.userId,
    required this.name,
    this.specialtyName,
    this.cabinetAddress,
    required this.ratingAverage,
    required this.ratingCount,
  });

  factory DoctorSummary.fromJson(Map<String, dynamic> json) =>
      _$DoctorSummaryFromJson(json);

  final int userId;
  final String name;
  final String? specialtyName;
  final String? cabinetAddress;
  final double ratingAverage;
  final int ratingCount;
}

@JsonSerializable(createToJson: false)
class DoctorResponse {
  const DoctorResponse({
    required this.userId,
    required this.email,
    required this.name,
    this.specialty,
    this.cabinetAddress,
    this.workingHours,
    this.phone,
    this.bio,
    required this.acceptanceMode,
    required this.slotDurationMinutes,
    required this.ratingAverage,
    required this.ratingCount,
  });

  factory DoctorResponse.fromJson(Map<String, dynamic> json) =>
      _$DoctorResponseFromJson(json);

  final int userId;
  final String email;
  final String name;
  final SpecialtyResponse? specialty;
  final String? cabinetAddress;
  final String? workingHours;
  final String? phone;
  final String? bio;
  final AcceptanceMode acceptanceMode;
  final int slotDurationMinutes;
  final double ratingAverage;
  final int ratingCount;
}

@JsonSerializable(createToJson: false)
class ReviewResponse {
  const ReviewResponse({
    required this.id,
    required this.appointmentId,
    required this.doctorId,
    required this.patientId,
    required this.patientName,
    required this.rating,
    this.comment,
    required this.createdAt,
    this.updatedAt,
  });

  factory ReviewResponse.fromJson(Map<String, dynamic> json) =>
      _$ReviewResponseFromJson(json);

  final int id;
  final int appointmentId;
  final int doctorId;
  final int patientId;
  final String patientName;
  final int rating;
  final String? comment;

  /// UTC instant (unlike appointment times, which are clinic wall-clock).
  final DateTime createdAt;
  final DateTime? updatedAt;
}
