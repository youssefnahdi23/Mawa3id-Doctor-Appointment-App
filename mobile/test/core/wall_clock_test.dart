import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/time/wall_clock.dart';

void main() {
  group('WallClock', () {
    test('parses a zone-less string without shifting the wall time', () {
      final parsed = WallClock.parse('2026-07-13T09:30:00');
      expect(parsed.year, 2026);
      expect(parsed.month, 7);
      expect(parsed.day, 13);
      expect(parsed.hour, 9);
      expect(parsed.minute, 30);
      expect(parsed.isUtc, isFalse);
    });

    test('round-trips parse → format byte-identically', () {
      const wire = '2026-12-31T23:05:00';
      expect(WallClock.format(WallClock.parse(wire)), wire);
    });

    test('format pads single-digit components', () {
      expect(WallClock.format(DateTime(2026, 1, 2, 3, 4, 5)),
          '2026-01-02T03:04:05');
    });

    test('rejects UTC-marked strings instead of silently converting', () {
      expect(() => WallClock.parse('2026-07-13T09:30:00Z'),
          throwsFormatException);
    });

    test('converter delegates to parse/format', () {
      const converter = WallClockConverter();
      expect(converter.toJson(converter.fromJson('2026-07-13T09:30:00')),
          '2026-07-13T09:30:00');
    });
  });
}
