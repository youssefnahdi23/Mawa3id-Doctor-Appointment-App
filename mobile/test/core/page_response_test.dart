import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/models/page_response.dart';

void main() {
  group('PageResponse', () {
    final json = {
      'content': [
        {'value': 'a'},
        {'value': 'b'},
      ],
      'page': {'size': 20, 'number': 0, 'totalElements': 2, 'totalPages': 1},
    };

    test('parses content through the item factory and the page meta', () {
      final page = PageResponse.fromJson(
          json, (item) => item['value'] as String);
      expect(page.content, ['a', 'b']);
      expect(page.page.size, 20);
      expect(page.page.number, 0);
      expect(page.page.totalElements, 2);
      expect(page.page.totalPages, 1);
    });

    test('isLast is true on the final page', () {
      final page = PageResponse.fromJson(json, (item) => item);
      expect(page.isLast, isTrue);
    });

    test('isLast is false when more pages exist', () {
      final page = PageResponse.fromJson({
        'content': <Map<String, dynamic>>[],
        'page': {
          'size': 20,
          'number': 0,
          'totalElements': 45,
          'totalPages': 3
        },
      }, (item) => item);
      expect(page.isLast, isFalse);
    });

    test('isLast is true for an empty zero-page result', () {
      final page = PageResponse.fromJson({
        'content': <Map<String, dynamic>>[],
        'page': {'size': 20, 'number': 0, 'totalElements': 0, 'totalPages': 0},
      }, (item) => item);
      expect(page.isLast, isTrue);
    });
  });
}
