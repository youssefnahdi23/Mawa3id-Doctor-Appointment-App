import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/availability/data/availability_models.dart';
import 'package:mawa3id/features/availability/data/availability_repository.dart';
import 'package:mawa3id/features/notifications/data/notification_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

Response<T> _ok<T>(T data) =>
    Response(requestOptions: RequestOptions(), data: data, statusCode: 200);

void main() {
  late _MockDio dio;

  setUp(() => dio = _MockDio());

  group('AvailabilityRepository', () {
    test('forDoctor() parses the rule list', () async {
      when(() => dio.get<List<dynamic>>(any())).thenAnswer((_) async => _ok([
            {
              'dayOfWeek': 'MONDAY',
              'startTime': '09:00:00',
              'endTime': '17:00:00'
            },
          ]));

      final rules = await AvailabilityRepository(dio).forDoctor(3);

      expect(rules.single.dayOfWeek, Weekday.monday);
      verify(() => dio.get<List<dynamic>>('/api/doctors/3/availability'))
          .called(1);
    });

    test('updateMine() PUTs the wholesale {rules: [...]} body', () async {
      when(() =>
              dio.put<List<dynamic>>(any(), data: any(named: 'data')))
          .thenAnswer((_) async => _ok([
                {
                  'dayOfWeek': 'FRIDAY',
                  'startTime': '08:00:00',
                  'endTime': '12:00:00'
                },
              ]));

      final saved = await AvailabilityRepository(dio).updateMine(const [
        AvailabilityRule(
            dayOfWeek: Weekday.friday, startTime: '08:00', endTime: '12:00'),
      ]);

      expect(saved.single.startMinutes, 8 * 60);
      final captured = verify(() => dio.put<List<dynamic>>(captureAny(),
          data: captureAny(named: 'data'))).captured;
      expect(captured[0], '/api/doctors/me/availability');
      expect(captured[1], {
        'rules': [
          {'dayOfWeek': 'FRIDAY', 'startTime': '08:00', 'endTime': '12:00'},
        ],
      });
    });
  });

  group('NotificationRepository', () {
    test('list() adds the unread flag only when filtering', () async {
      when(() => dio.get<Map<String, dynamic>>(any(),
              queryParameters: any(named: 'queryParameters')))
          .thenAnswer((_) async => _ok({
                'content': <Map<String, dynamic>>[],
                'page': {
                  'size': 20,
                  'number': 0,
                  'totalElements': 0,
                  'totalPages': 0
                },
              }));

      await NotificationRepository(dio).list(unreadOnly: true);
      var captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
              queryParameters: captureAny(named: 'queryParameters')))
          .captured;
      expect(captured[0], '/api/notifications');
      expect(captured[1], {'unread': true, 'page': 0, 'size': 20});

      await NotificationRepository(dio).list();
      captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
              queryParameters: captureAny(named: 'queryParameters')))
          .captured;
      expect((captured[1] as Map).containsKey('unread'), isFalse);
    });

    test('unreadCount() unwraps {count}', () async {
      when(() => dio.get<Map<String, dynamic>>(any()))
          .thenAnswer((_) async => _ok({'count': 4}));

      expect(await NotificationRepository(dio).unreadCount(), 4);
      verify(() =>
              dio.get<Map<String, dynamic>>('/api/notifications/unread-count'))
          .called(1);
    });

    test('markRead() and markAllRead() PUT and parse', () async {
      when(() => dio.put<Map<String, dynamic>>('/api/notifications/9/read'))
          .thenAnswer((_) async => _ok({
                'id': 9,
                'type': 'APPOINTMENT_REMINDER',
                'message': 'Tomorrow 09:30',
                'appointmentId': 11,
                'read': true,
                'createdAt': '2026-07-13T08:00:00Z',
              }));
      when(() => dio.put<Map<String, dynamic>>('/api/notifications/read-all'))
          .thenAnswer((_) async => _ok({'updated': 3}));

      final repository = NotificationRepository(dio);
      expect((await repository.markRead(9)).read, isTrue);
      expect(await repository.markAllRead(), 3);
    });
  });
}
