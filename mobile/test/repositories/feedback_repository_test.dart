import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/community/data/feedback_models.dart';
import 'package:mawa3id/features/community/data/feedback_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

void main() {
  test('submit() posts the category, message, and client metadata', () async {
    final dio = _MockDio();
    final repository = FeedbackRepository(dio);
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => Response(
            requestOptions: RequestOptions(), statusCode: 201, data: {}));

    await repository.submit(
        category: FeedbackCategory.bug, message: 'slot picker crashes');

    final captured = verify(() => dio.post<Map<String, dynamic>>(captureAny(),
        data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/feedback');
    final body = captured[1] as Map<String, dynamic>;
    expect(body['category'], 'BUG');
    expect(body['message'], 'slot picker crashes');
    expect(body['appVersion'], '1.0.0');
    // Flutter's test binding reports the Android target platform by default.
    expect(body['platform'], 'ANDROID');
  });
}
