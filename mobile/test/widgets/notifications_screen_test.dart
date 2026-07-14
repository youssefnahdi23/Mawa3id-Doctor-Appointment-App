import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/models/page_response.dart';
import 'package:mawa3id/features/notifications/data/notification_models.dart';
import 'package:mawa3id/features/notifications/data/notification_repository.dart';
import 'package:mawa3id/features/notifications/ui/notifications_screen.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockNotificationRepository extends Mock
    implements NotificationRepository {}

NotificationResponse _notification({required bool read}) =>
    NotificationResponse(
      id: 9,
      type: NotificationType.appointmentAccepted,
      message: 'Your appointment was accepted',
      appointmentId: 11,
      read: read,
      createdAt: DateTime.now().toUtc().subtract(const Duration(minutes: 5)),
    );

void main() {
  late _MockNotificationRepository repository;

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    repository = _MockNotificationRepository();
    when(() => repository.unreadCount()).thenAnswer((_) async => 0);
  });

  void stubList(List<NotificationResponse> items) {
    when(() => repository.list(page: any(named: 'page')))
        .thenAnswer((_) async => PageResponse(
              content: items,
              page: PageMeta(
                  size: 20,
                  number: 0,
                  totalElements: items.length,
                  totalPages: 1),
            ));
  }

  testWidgets('tapping an unread notification marks it read',
      (tester) async {
    stubList([_notification(read: false)]);
    when(() => repository.markRead(9))
        .thenAnswer((_) async => _notification(read: true));

    await tester.pumpWidget(
        wrapWithApp(const NotificationsScreen(), overrides: [
      notificationRepositoryProvider.overrideWithValue(repository),
    ]));
    await tester.pumpAndSettle();

    expect(find.text('Your appointment was accepted'), findsOneWidget);
    expect(find.textContaining('min ago'), findsOneWidget);

    await tester.tap(find.text('Your appointment was accepted'));
    await tester.pumpAndSettle();

    verify(() => repository.markRead(9)).called(1);
  });

  testWidgets('read notifications are not tappable', (tester) async {
    stubList([_notification(read: true)]);

    await tester.pumpWidget(
        wrapWithApp(const NotificationsScreen(), overrides: [
      notificationRepositoryProvider.overrideWithValue(repository),
    ]));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Your appointment was accepted'));
    await tester.pumpAndSettle();

    verifyNever(() => repository.markRead(any()));
  });

  testWidgets('mark-all action calls through and shows the empty state after',
      (tester) async {
    stubList([_notification(read: false)]);
    when(() => repository.markAllRead()).thenAnswer((_) async => 1);

    await tester.pumpWidget(
        wrapWithApp(const NotificationsScreen(), overrides: [
      notificationRepositoryProvider.overrideWithValue(repository),
    ]));
    await tester.pumpAndSettle();

    stubList([_notification(read: true)]);
    await tester.tap(find.byTooltip('Mark all as read'));
    await tester.pumpAndSettle();

    verify(() => repository.markAllRead()).called(1);
  });
}
