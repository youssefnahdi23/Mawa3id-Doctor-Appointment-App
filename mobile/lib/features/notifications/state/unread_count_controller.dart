import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/state/session_controller.dart';
import '../data/notification_repository.dart';

final unreadCountProvider =
    AsyncNotifierProvider<UnreadCountController, int>(
        UnreadCountController.new);

/// Unread-notification badge count. Rebuilds on login/logout; explicitly
/// [refresh]ed on tab changes and after notification mutations — no polling.
class UnreadCountController extends AsyncNotifier<int> {
  @override
  Future<int> build() async {
    final session = await ref.watch(sessionControllerProvider.future);
    if (session == null) return 0;
    return ref.read(notificationRepositoryProvider).unreadCount();
  }

  Future<void> refresh() async {
    final session = ref.read(sessionControllerProvider).valueOrNull;
    if (session == null) return;
    final result = await AsyncValue.guard(
        () => ref.read(notificationRepositoryProvider).unreadCount());
    // Keep the last known count on a failed background refresh.
    if (result.hasValue) state = result;
  }
}
