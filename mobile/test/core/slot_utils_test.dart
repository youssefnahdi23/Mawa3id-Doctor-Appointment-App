import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/appointments/data/appointment_repository.dart';
import 'package:mawa3id/features/appointments/logic/slot_utils.dart';

SlotResponse _slot(String start) => SlotResponse(
      start: DateTime.parse(start),
      end: DateTime.parse(start).add(const Duration(minutes: 30)),
    );

void main() {
  group('groupSlotsByDay', () {
    test('buckets by calendar day, preserving in-day order', () {
      final grouped = groupSlotsByDay([
        _slot('2026-07-14T09:00:00'),
        _slot('2026-07-14T09:30:00'),
        _slot('2026-07-15T11:00:00'),
      ]);
      expect(grouped.keys,
          [DateTime(2026, 7, 14), DateTime(2026, 7, 15)]);
      expect(grouped[DateTime(2026, 7, 14)]!.map((s) => s.start.hour),
          [9, 9]);
      expect(grouped[DateTime(2026, 7, 15)]!.single.start.hour, 11);
    });

    test('returns an empty map for no slots', () {
      expect(groupSlotsByDay(const []), isEmpty);
    });
  });

  group('clampSlotRangeEnd', () {
    final from = DateTime(2026, 7, 13);

    test('leaves ranges within 31 days untouched', () {
      final to = DateTime(2026, 7, 26);
      expect(clampSlotRangeEnd(from: from, to: to), to);
    });

    test('clamps ranges beyond 31 inclusive days', () {
      final clamped =
          clampSlotRangeEnd(from: from, to: DateTime(2026, 12, 1));
      // 31 inclusive days: Jul 13 + 30 = Aug 12.
      expect(clamped, DateTime(2026, 8, 12));
    });

    test('accepts exactly 31 inclusive days', () {
      final to = DateTime(2026, 8, 12);
      expect(clampSlotRangeEnd(from: from, to: to), to);
    });
  });

  test('statusApiName produces backend enum names', () {
    expect(statusApiName(AppointmentStatus.pending), 'PENDING');
    expect(statusApiName(AppointmentStatus.cancelled), 'CANCELLED');
  });
}
