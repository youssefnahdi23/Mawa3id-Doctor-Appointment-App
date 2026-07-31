import 'package:json_annotation/json_annotation.dart';

part 'patient_models.g.dart';

/// The signed-in patient's own profile (`GET /api/patients/me`). `email` and
/// `patientCode` are read-only; only [fullName] and [dateOfBirth] are editable.
@JsonSerializable(createToJson: false)
class PatientResponse {
  const PatientResponse({
    required this.userId,
    required this.email,
    required this.fullName,
    required this.patientCode,
    this.dateOfBirth,
    this.phone,
  });

  factory PatientResponse.fromJson(Map<String, dynamic> json) =>
      _$PatientResponseFromJson(json);

  final int userId;
  final String email;
  final String fullName;
  final String patientCode;

  /// Backend `LocalDate` (`yyyy-MM-dd`); nullable for parity with the API.
  final DateTime? dateOfBirth;

  final String? phone;
}
