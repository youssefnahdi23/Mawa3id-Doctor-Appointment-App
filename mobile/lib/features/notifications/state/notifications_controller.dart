import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/notification_models.dart';
import '../data/notification_repository.dart';
import 'unread_count_controller.dart';

class NotificationListData {
  const NotificationListData({
    required this.items,
    required this.isLast,
    required this.nextPage,
    this.loadingMore = false,
  });

  final List<NotificationResponse> items;
  final bool isLast;
  final int nextPage;
  final bool loadingMore;

  NotificationListData copyWith({
    List<NotificationResponse>? items,
    bool? isLast,
    int? nextPage,
    bool? loadingMore,
  }) {
    return NotificationListData(
      items: items ?? this.items,
      isLast: isLast ?? this.isLast,
      nextPage: nextPage ?? this.nextPage,
      loadingMore: loadingMore ?? this.loadingMore,
    );
  }
}

final notificationsControllerProvider = AsyncNotifierProvider<
    NotificationsController, NotificationListData>(NotificationsController.new);

class NotificationsController extends AsyncNotifier<NotificationListData> {
  @override
  Future<NotificationListData> build() => _fetch(0);

  Future<NotificationListData> _fetch(int page) async {
    final result =
        await ref.read(notificationRepositoryProvider).list(page: page);
    return NotificationListData(
      items: result.content,
      isLast: result.isLast,
      nextPage: page + 1,
    );
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null || current.isLast || current.loadingMore) return;
    state = AsyncData(current.copyWith(loadingMore: true));
    try {
      final next = await _fetch(current.nextPage);
      state = AsyncData(NotificationListData(
        items: [...current.items, ...next.items],
        isLast: next.isLast,
        nextPage: current.nextPage + 1,
      ));
    } catch (_) {
      state = AsyncData(current.copyWith(loadingMore: false));
    }
  }

  Future<void> markRead(int id) async {
    final updated =
        await ref.read(notificationRepositoryProvider).markRead(id);
    final current = state.valueOrNull;
    if (current != null) {
      state = AsyncData(current.copyWith(
        items: [
          for (final item in current.items)
            item.id == id ? updated : item,
        ],
      ));
    }
    await ref.read(unreadCountProvider.notifier).refresh();
  }

  Future<void> markAllRead() async {
    await ref.read(notificationRepositoryProvider).markAllRead();
    ref.invalidateSelf();
    await ref.read(unreadCountProvider.notifier).refresh();
  }
}
