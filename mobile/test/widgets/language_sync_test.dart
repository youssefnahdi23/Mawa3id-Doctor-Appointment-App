import 'package:dio/dio.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/l10n/language_sync.dart';
import 'package:mawa3id/core/l10n/locale_controller.dart';
import 'package:mawa3id/core/network/dio_client.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockDio extends Mock implements Dio {}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(() => SharedPreferences.setMockInitialValues({}));

  const session = Session(
    userId: 1,
    username: 'sami',
    email: 'a@b.c',
    role: UserRole.patient,
  );

  Response<void> ok() =>
      Response<void>(requestOptions: RequestOptions(path: ''), statusCode: 204);

  test('PUTs the language to the backend when locale changes while signed in',
      () async {
    final dio = _MockDio();
    when(() => dio.put<void>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => ok());

    final container = ProviderContainer(overrides: [
      dioProvider.overrideWithValue(dio),
      sessionControllerProvider.overrideWith(() => FakeSessionController(session)),
    ]);
    addTearDown(container.dispose);

    container.read(languageSyncProvider); // activate the listeners
    await container.read(sessionControllerProvider.future);
    container.read(localeControllerProvider.notifier).set(const Locale('ar'));
    await Future<void>.delayed(const Duration(milliseconds: 10));

    verify(() => dio.put<void>('/api/users/me/language', data: {'language': 'ar'}))
        .called(greaterThanOrEqualTo(1));
  });

  test('does not sync when signed out', () async {
    final dio = _MockDio();
    when(() => dio.put<void>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => ok());

    final container = ProviderContainer(overrides: [
      dioProvider.overrideWithValue(dio),
      // Default fake session is null (signed out).
      sessionControllerProvider.overrideWith(FakeSessionController.new),
    ]);
    addTearDown(container.dispose);

    container.read(languageSyncProvider);
    await container.read(sessionControllerProvider.future);
    container.read(localeControllerProvider.notifier).set(const Locale('fr'));
    await Future<void>.delayed(const Duration(milliseconds: 10));

    verifyNever(() => dio.put<void>(any(), data: any(named: 'data')));
  });
}
