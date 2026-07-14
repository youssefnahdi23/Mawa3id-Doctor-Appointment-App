import 'package:json_annotation/json_annotation.dart';

part 'availability_models.g.dart';

/// Java `DayOfWeek` names, ordered Monday-first like the backend.
enum Weekday {
  @JsonValue('MONDAY')
  monday,
  @JsonValue('TUESDAY')
  tuesday,
  @JsonValue('WEDNESDAY')
  wednesday,
  @JsonValue('THURSDAY')
  thursday,
  @JsonValue('FRIDAY')
  friday,
  @JsonValue('SATURDAY')
  saturday,
  @JsonValue('SUNDAY')
  sunday,
}

/// One weekly availability rule. Same shape in requests and responses.
///
/// Times are zone-less `HH:mm[:ss]` strings (Java `LocalTime`); they are kept
/// as strings in the model and converted to minutes for editing/validation.
@JsonSerializable()
class AvailabilityRule {
  const AvailabilityRule({
    required this.dayOfWeek,
    required this.startTime,
    required this.endTime,
  });

  factory AvailabilityRule.fromJson(Map<String, dynamic> json) =>
      _$AvailabilityRuleFromJson(json);

  Map<String, dynamic> toJson() => _$AvailabilityRuleToJson(this);

  final Weekday dayOfWeek;
  final String startTime;
  final String endTime;

  int get startMinutes => parseTimeToMinutes(startTime);
  int get endMinutes => parseTimeToMinutes(endTime);
}

/// Parses `HH:mm` or `HH:mm:ss` into minutes since midnight.
int parseTimeToMinutes(String time) {
  final parts = time.split(':');
  return int.parse(parts[0]) * 60 + int.parse(parts[1]);
}

/// Formats minutes since midnight as `HH:mm` (what the backend accepts).
String formatMinutesAsTime(int minutes) {
  final h = (minutes ~/ 60).toString().padLeft(2, '0');
  final m = (minutes % 60).toString().padLeft(2, '0');
  return '$h:$m';
}
