import 'package:json_annotation/json_annotation.dart';

import '../../../core/time/wall_clock.dart';

part 'record_models.g.dart';

/// A doctor's visit record for one appointment: a required diagnosis plus
/// optional clinical detail. Attaching one to an ACCEPTED appointment
/// auto-completes it on the backend.
@JsonSerializable(createToJson: false)
class MedicalRecordResponse {
  const MedicalRecordResponse({
    required this.id,
    required this.appointmentId,
    required this.doctorId,
    required this.doctorName,
    required this.patientId,
    required this.patientName,
    required this.visitStart,
    required this.diagnosis,
    this.notes,
    this.prescription,
    this.followUpDate,
    required this.createdAt,
    this.updatedAt,
  });

  factory MedicalRecordResponse.fromJson(Map<String, dynamic> json) =>
      _$MedicalRecordResponseFromJson(json);

  final int id;
  final int appointmentId;
  final int doctorId;
  final String doctorName;
  final int patientId;
  final String patientName;

  /// Clinic wall-clock time of the visit — display as-is, never zone-convert.
  @WallClockConverter()
  final DateTime visitStart;

  final String diagnosis;
  final String? notes;
  final String? prescription;

  /// Optional follow-up day (date only, no time).
  final DateTime? followUpDate;

  /// UTC instant (unlike [visitStart], which is clinic wall-clock).
  final DateTime createdAt;
  final DateTime? updatedAt;
}

/// Payload for creating or updating a medical record. Only [diagnosis] is
/// required; empty optional fields are omitted from the request body.
class RecordRequest {
  const RecordRequest({
    required this.diagnosis,
    this.notes,
    this.prescription,
    this.followUpDate,
  });

  final String diagnosis;
  final String? notes;
  final String? prescription;
  final DateTime? followUpDate;

  Map<String, dynamic> toJson() => {
        'diagnosis': diagnosis,
        if (notes != null && notes!.isNotEmpty) 'notes': notes,
        if (prescription != null && prescription!.isNotEmpty)
          'prescription': prescription,
        if (followUpDate != null) 'followUpDate': _formatDate(followUpDate!),
      };
}

/// `yyyy-MM-dd` — the LocalDate wire format the backend expects.
String _formatDate(DateTime date) {
  String two(int n) => n.toString().padLeft(2, '0');
  return '${date.year.toString().padLeft(4, '0')}-${two(date.month)}-${two(date.day)}';
}
