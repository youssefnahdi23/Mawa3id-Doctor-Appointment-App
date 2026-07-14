import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';
import 'package:mawa3id/features/notifications/data/notification_repository.dart';
import 'package:mawa3id/features/notifications/state/unread_count_controller.dart';
import 'package:mocktail/mocktail.dart';

class _MockNotificationRepository extends Mock
    implements NotificationRepository {}

class _FakeSessionController extends SessionController {
  _FakeSessionController(this._session);

  final Session? _session;

  @override
  Future<Session?> build() async => _session;
}

void main() {
  late _MockNotificationRepository repository;

  const session =
      Session(userId: 7, email: 'a@b.c', role: UserRole.patient);

  ProviderContainer buildContainer(Session? sessionValue) {
    final container = ProviderContainer(overrides: [
      sessionControllerProvider
          .overrideWith(() => _FakeSessionController(sessionValue)),
      notificationRepositoryProvider.overrideWithValue(repository),
    ]);
    addTearDown(container.dispose);
    return container;
  }

  setUp(() => repository = _MockNotificationRepository());

  test('logged out → count 0 without hitting the API', () async {
    final container = buildContainer(null);

    expect(await container.read(unreadCountProvider.future), 0);
    verifyNever(() => repository.unreadCount());
  });

  test('logged in → fetches the unread count', () async {
    when(() => repository.unreadCount()).thenAnswer((_) async => 5);
    final container = buildContainer(session);

    expect(await container.read(unreadCountProvider.future), 5);
  });

  test('refresh() updates the count', () async {
    when(() => repository.unreadCount()).thenAnswer((_) async => 5);
    final container = buildContainer(session);
    await container.read(unreadCountProvider.future);

    when(() => repository.unreadCount()).thenAnswer((_) async => 2);
    await container.read(unreadCountProvider.notifier).refresh();

    expect(container.read(unreadCountProvider).value, 2);
  });

  test('failed refresh keeps the last known count', () async {
    when(() => repository.unreadCount()).thenAnswer((_) async => 5);
    final container = buildContainer(session);
    await container.read(unreadCountProvider.future);

    when(() => repository.unreadCount()).thenThrow(Exception('offline'));
    await container.read(unreadCountProvider.notifier).refresh();

    expect(container.read(unreadCountProvider).value, 5);
  });
}
