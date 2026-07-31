import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/appointments/data/appointment_models.dart';
import 'package:mawa3id/features/appointments/data/appointment_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

final _appointmentJson = {
  'id': 11,
  'doctorId': 3,
  'doctorName': 'Dr. Amal',
  'patientId': 7,
  'patientName': 'Sami',
  'start': '2026-07-14T09:30:00',
  'end': '2026-07-14T10:00:00',
  'status': 'PENDING',
  'reason': null,
};

Response<T> _ok<T>(T data) =>
    Response(requestOptions: RequestOptions(), data: data, statusCode: 200);

void main() {
  late _MockDio dio;
  late AppointmentRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = AppointmentRepository(dio);
  });

  test('slots() formats dates and clamps ranges over 31 days', () async {
    when(() => dio.get<List<dynamic>>(any(),
            queryParameters: any(named: 'queryParameters')))
        .thenAnswer((_) async => _ok([
              {'start': '2026-07-14T09:00:00', 'end': '2026-07-14T09:30:00'},
            ]));

    final slots = await repository.slots(3,
        from: DateTime(2026, 7, 13), to: DateTime(2026, 12, 1));

    expect(slots.single.start.hour, 9);
    final captured = verify(() => dio.get<List<dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[0], '/api/doctors/3/slots');
    expect(captured[1], {'from': '2026-07-13', 'to': '2026-08-12'});
  });

  test('book() sends wall-clock startTime and omits empty reason', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_appointmentJson));

    final booked = await repository.book(
      doctorId: 3,
      startTime: DateTime(2026, 7, 14, 9, 30),
    );

    expect(booked.status, AppointmentStatus.pending);
    final captured = verify(() => dio.post<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/appointments');
    expect(captured[1],
        {'doctorId': 3, 'startTime': '2026-07-14T09:30:00'});
  });

  test('mine() and queue() hit their endpoints with a status filter',
      () async {
    when(() => dio.get<Map<String, dynamic>>(any(),
            queryParameters: any(named: 'queryParameters')))
        .thenAnswer((_) async => _ok({
              'content': [_appointmentJson],
              'page': {
                'size': 20,
                'number': 0,
                'totalElements': 1,
                'totalPages': 1
              },
            }));

    await repository.mine(status: AppointmentStatus.accepted);
    var captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[0], '/api/appointments/me');
    expect((captured[1] as Map)['status'], 'ACCEPTED');

    await repository.queue();
    captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[0], '/api/appointments');
    expect((captured[1] as Map).containsKey('status'), isFalse);
  });

  test('transitions PUT to /api/appointments/{id}/{action}', () async {
    when(() => dio.put<Map<String, dynamic>>(any()))
        .thenAnswer((_) async => _ok({..._appointmentJson,
              'status': 'ACCEPTED'}));

    final accepted = await repository.accept(11);
    expect(accepted.status, AppointmentStatus.accepted);
    verify(() => dio.put<Map<String, dynamic>>('/api/appointments/11/accept'))
        .called(1);

    await repository.reject(11);
    verify(() => dio.put<Map<String, dynamic>>('/api/appointments/11/reject'))
        .called(1);
    await repository.complete(11);
    verify(() =>
            dio.put<Map<String, dynamic>>('/api/appointments/11/complete'))
        .called(1);
    await repository.cancel(11);
    verify(() => dio.put<Map<String, dynamic>>('/api/appointments/11/cancel'))
        .called(1);
    await repository.noShow(11);
    verify(() =>
            dio.put<Map<String, dynamic>>('/api/appointments/11/no-show'))
        .called(1);
  });

  test('reschedule() PUTs the new wall-clock startTime', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok({..._appointmentJson, 'status': 'ACCEPTED'}));

    await repository.reschedule(11, DateTime(2026, 7, 14, 10, 30));

    final captured = verify(() => dio.put<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/appointments/11/reschedule');
    expect(captured[1], {'startTime': '2026-07-14T10:30:00'});
  });

  test('status filter uses the NO_SHOW wire name', () async {
    when(() => dio.get<Map<String, dynamic>>(any(),
            queryParameters: any(named: 'queryParameters')))
        .thenAnswer((_) async => _ok({
              'content': [],
              'page': {
                'size': 20,
                'number': 0,
                'totalElements': 0,
                'totalPages': 0
              },
            }));

    await repository.queue(status: AppointmentStatus.noShow);

    final captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect((captured[1] as Map)['status'], 'NO_SHOW');
  });
}
