import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/records/data/record_models.dart';
import 'package:mawa3id/features/records/data/record_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

final _recordJson = {
  'id': 5,
  'appointmentId': 11,
  'doctorId': 3,
  'doctorName': 'Dr. Amal',
  'patientId': 7,
  'patientName': 'Sami',
  'visitStart': '2026-07-14T09:30:00',
  'diagnosis': 'Seasonal flu',
  'notes': 'Rest and fluids',
  'prescription': 'Paracetamol 500mg',
  'followUpDate': '2026-07-21',
  'createdAt': '2026-07-14T10:00:00Z',
  'updatedAt': null,
};

Response<T> _ok<T>(T data, [int status = 200]) =>
    Response(requestOptions: RequestOptions(), data: data, statusCode: status);

DioException _dioError(int status) => DioException(
      requestOptions: RequestOptions(),
      response: Response(requestOptions: RequestOptions(), statusCode: status),
    );

void main() {
  late _MockDio dio;
  late RecordRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = RecordRepository(dio);
  });

  test('create() posts diagnosis and omits empty optional fields', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_recordJson, 201));

    final record = await repository.create(
        11, const RecordRequest(diagnosis: 'Seasonal flu', notes: ''));

    expect(record.diagnosis, 'Seasonal flu');
    expect(record.visitStart.hour, 9);
    expect(record.followUpDate, DateTime(2026, 7, 21));
    final captured = verify(() => dio.post<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/appointments/11/record');
    expect(captured[1], {'diagnosis': 'Seasonal flu'});
  });

  test('create() serializes followUpDate as yyyy-MM-dd', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_recordJson, 201));

    await repository.create(
      11,
      RecordRequest(
        diagnosis: 'x',
        prescription: 'y',
        followUpDate: DateTime(2026, 7, 21),
      ),
    );
    final captured = verify(() => dio.post<Map<String, dynamic>>(any(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], {
      'diagnosis': 'x',
      'prescription': 'y',
      'followUpDate': '2026-07-21',
    });
  });

  test('update() PUTs to the record endpoint', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_recordJson));

    await repository.update(11, const RecordRequest(diagnosis: 'Updated'));
    verify(() => dio.put<Map<String, dynamic>>(
        '/api/appointments/11/record', data: any(named: 'data'))).called(1);
  });

  test('get() returns the record when present', () async {
    when(() => dio.get<Map<String, dynamic>>(any()))
        .thenAnswer((_) async => _ok(_recordJson));

    final record = await repository.get(11);
    expect(record, isNotNull);
    expect(record!.appointmentId, 11);
  });

  test('get() maps a 404 to null', () async {
    when(() => dio.get<Map<String, dynamic>>(any())).thenThrow(_dioError(404));

    expect(await repository.get(11), isNull);
  });

  test('mine() pages the record history', () async {
    when(() => dio.get<Map<String, dynamic>>(any(),
            queryParameters: any(named: 'queryParameters')))
        .thenAnswer((_) async => _ok({
              'content': [_recordJson],
              'page': {
                'size': 20,
                'number': 0,
                'totalElements': 1,
                'totalPages': 1
              },
            }));

    final page = await repository.mine();
    expect(page.content.single.diagnosis, 'Seasonal flu');
    final captured = verify(() => dio.get<Map<String, dynamic>>(captureAny(),
            queryParameters: captureAny(named: 'queryParameters')))
        .captured;
    expect(captured[0], '/api/records/me');
    expect(captured[1], {'page': 0, 'size': 20});
  });
}
