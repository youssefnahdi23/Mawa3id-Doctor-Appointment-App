import 'package:json_annotation/json_annotation.dart';

/// Clinic-local wall-clock times.
///
/// The backend sends appointment and slot times as zone-less ISO-8601 strings
/// (e.g. `2026-07-13T09:30:00`) that mean "clinic local time". They must be
/// treated as opaque wall-clock values: never convert them with
/// [DateTime.toUtc] or [DateTime.toLocal], and never attach a zone when
/// serializing. The only zoned timestamp in the API is
/// `NotificationResponse.createdAt` (a UTC instant), which is parsed
/// normally, not through this class.
class WallClock {
  WallClock._();

  /// Parses a zone-less ISO-8601 string into a wall-clock [DateTime].
  ///
  /// Throws [FormatException] if the string carries a UTC/offset marker,
  /// because silently accepting one would reintroduce zone conversions.
  static DateTime parse(String value) {
    final parsed = DateTime.parse(value);
    if (parsed.isUtc) {
      throw FormatException('Expected a zone-less wall-clock time', value);
    }
    return parsed;
  }

  /// Formats as `yyyy-MM-ddTHH:mm:ss` (what the backend expects).
  static String format(DateTime value) {
    String two(int n) => n.toString().padLeft(2, '0');
    final date =
        '${value.year.toString().padLeft(4, '0')}-${two(value.month)}-${two(value.day)}';
    return '${date}T${two(value.hour)}:${two(value.minute)}:${two(value.second)}';
  }
}

/// json_serializable converter for wall-clock date-time fields.
class WallClockConverter implements JsonConverter<DateTime, String> {
  const WallClockConverter();

  @override
  DateTime fromJson(String json) => WallClock.parse(json);

  @override
  String toJson(DateTime object) => WallClock.format(object);
}
