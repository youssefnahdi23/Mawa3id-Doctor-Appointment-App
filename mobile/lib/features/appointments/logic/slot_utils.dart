import '../data/appointment_models.dart';

/// Groups slots by calendar day (key: midnight wall-clock [DateTime]),
/// preserving the backend's chronological order within each day.
Map<DateTime, List<SlotResponse>> groupSlotsByDay(List<SlotResponse> slots) {
  final grouped = <DateTime, List<SlotResponse>>{};
  for (final slot in slots) {
    final day =
        DateTime(slot.start.year, slot.start.month, slot.start.day);
    grouped.putIfAbsent(day, () => []).add(slot);
  }
  return grouped;
}
