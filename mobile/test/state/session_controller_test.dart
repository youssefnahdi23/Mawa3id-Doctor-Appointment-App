import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/network/api_exception.dart';
import 'package:mawa3id/core/network/dio_client.dart';
import 'package:mawa3id/core/storage/token_storage.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/data/auth_repository.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';
import 'package:mocktail/mocktail.dart';

class _MockTokenStorage extends Mock implements TokenStorage {}

class _MockAuthRepository extends Mock implements AuthRepository {}

StoredSession _stored({DateTime? expiresAt, String? refreshToken}) =>
    StoredSession(
      token: 'jwt',
      refreshToken: refreshToken,
      userId: 7,
      username: 'sami',
      email: 'a@b.c',
      role: 'PATIENT',
      expiresAt: expiresAt ?? DateTime.now().add(const Duration(hours: 1)),
    );

void main() {
  late _MockTokenStorage storage;
  late _MockAuthRepository authRepository;
  late ProviderContainer container;

  setUpAll(() {
    registerFallbackValue(_stored());
  });

  setUp(() {
    storage = _MockTokenStorage();
    authRepository = _MockAuthRepository();
    when(() => storage.clear()).thenAnswer((_) async {});
    when(() => storage.save(any())).thenAnswer((_) async {});
    when(() => storage.cachedRefreshToken).thenReturn(null);
    container = ProviderContainer(overrides: [
      tokenStorageProvider.overrideWithValue(storage),
      authRepositoryProvider.overrideWithValue(authRepository),
    ]);
    addTearDown(container.dispose);
  });

  test('no stored token → logged out', () async {
    when(() => storage.read()).thenAnswer((_) async => null);

    expect(await container.read(sessionControllerProvider.future), isNull);
  });

  test('expired token without a refresh token → cleared and logged out',
      () async {
    when(() => storage.read()).thenAnswer((_) async => _stored(
        expiresAt: DateTime.now().subtract(const Duration(minutes: 1))));

    expect(await container.read(sessionControllerProvider.future), isNull);
    verify(() => storage.clear()).called(1);
  });

  test('expired token WITH a refresh token → still revalidates via /me',
      () async {
    when(() => storage.read()).thenAnswer((_) async => _stored(
          expiresAt: DateTime.now().subtract(const Duration(minutes: 1)),
          refreshToken: 'refresh-1',
        ));
    when(() => authRepository.me()).thenAnswer((_) async => const MeResponse(
        userId: 7, username: 'sami', email: 'a@b.c', role: UserRole.patient));

    final session = await container.read(sessionControllerProvider.future);

    expect(session!.userId, 7);
    verifyNever(() => storage.clear());
  });

  test('valid token accepted by /auth/me → session with fresh identity',
      () async {
    when(() => storage.read()).thenAnswer((_) async => _stored());
    when(() => authRepository.me()).thenAnswer((_) async => const MeResponse(
        userId: 7, username: 'sami', email: 'a@b.c', role: UserRole.patient));

    final session = await container.read(sessionControllerProvider.future);

    expect(session!.userId, 7);
    expect(session.username, 'sami');
    expect(session.role, UserRole.patient);
  });

  test('token rejected by /auth/me → cleared and logged out', () async {
    when(() => storage.read()).thenAnswer((_) async => _stored());
    when(() => authRepository.me()).thenThrow(
        const ApiException(kind: ApiErrorKind.unauthorized, statusCode: 401));

    expect(await container.read(sessionControllerProvider.future), isNull);
    verify(() => storage.clear()).called(1);
  });

  test('backend unreachable → falls back to the stored session', () async {
    when(() => storage.read()).thenAnswer((_) async => _stored());
    when(() => authRepository.me())
        .thenThrow(const ApiException(kind: ApiErrorKind.network));

    final session = await container.read(sessionControllerProvider.future);

    expect(session!.username, 'sami');
    verifyNever(() => storage.clear());
  });

  test('signIn persists the session (with refresh token) and updates state',
      () async {
    when(() => storage.read()).thenAnswer((_) async => null);
    await container.read(sessionControllerProvider.future);

    await container.read(sessionControllerProvider.notifier).signIn(
          const AuthResponse(
            token: 'jwt2',
            tokenType: 'Bearer',
            expiresInMs: 3600000,
            refreshToken: 'refresh-2',
            userId: 9,
            username: 'amal',
            email: 'doc@x.y',
            role: UserRole.doctor,
          ),
        );

    final saved =
        verify(() => storage.save(captureAny())).captured.single
            as StoredSession;
    expect(saved.token, 'jwt2');
    expect(saved.refreshToken, 'refresh-2');
    expect(saved.username, 'amal');
    expect(saved.role, 'DOCTOR');
    expect(saved.expiresAt.isAfter(DateTime.now()), isTrue);
    expect(container.read(sessionControllerProvider).value!.userId, 9);
  });

  test('logout clears storage and state', () async {
    when(() => storage.read()).thenAnswer((_) async => _stored());
    when(() => authRepository.me()).thenAnswer((_) async => const MeResponse(
        userId: 7, username: 'sami', email: 'a@b.c', role: UserRole.patient));
    await container.read(sessionControllerProvider.future);

    await container.read(sessionControllerProvider.notifier).logout();

    expect(container.read(sessionControllerProvider).value, isNull);
    verify(() => storage.clear()).called(1);
  });
}
