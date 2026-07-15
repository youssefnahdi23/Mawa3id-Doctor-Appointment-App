import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/reviews/data/review_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

final _reviewJson = {
  'id': 9,
  'appointmentId': 11,
  'doctorId': 3,
  'patientId': 7,
  'patientName': 'Sami',
  'rating': 4,
  'comment': 'Great visit',
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
  late ReviewRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = ReviewRepository(dio);
  });

  test('create() posts rating and comment', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_reviewJson, 201));

    final review = await repository.create(
        11, const ReviewRequest(rating: 4, comment: 'Great visit'));

    expect(review.rating, 4);
    final captured = verify(() => dio.post<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/appointments/11/review');
    expect(captured[1], {'rating': 4, 'comment': 'Great visit'});
  });

  test('create() omits an empty comment', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_reviewJson, 201));

    await repository.create(11, const ReviewRequest(rating: 5, comment: ''));
    final captured = verify(() => dio.post<Map<String, dynamic>>(any(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], {'rating': 5});
  });

  test('update() PUTs to the review endpoint', () async {
    when(() => dio.put<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok(_reviewJson));

    await repository.update(11, const ReviewRequest(rating: 3));
    verify(() => dio.put<Map<String, dynamic>>(
        '/api/appointments/11/review', data: any(named: 'data'))).called(1);
  });

  test('get() returns the review when present', () async {
    when(() => dio.get<Map<String, dynamic>>(any()))
        .thenAnswer((_) async => _ok(_reviewJson));

    final review = await repository.get(11);
    expect(review, isNotNull);
    expect(review!.rating, 4);
  });

  test('get() maps a 404 to null', () async {
    when(() => dio.get<Map<String, dynamic>>(any())).thenThrow(_dioError(404));

    expect(await repository.get(11), isNull);
  });
}
