import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/time/relative_time.dart';
import '../../../core/widgets/empty_state.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/loading_skeleton.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/notification_models.dart';
import '../state/notifications_controller.dart';

IconData notificationIcon(NotificationType type) {
  return switch (type) {
    NotificationType.appointmentBooked => Icons.event_available,
    NotificationType.appointmentAccepted => Icons.check_circle_outline,
    NotificationType.appointmentRejected => Icons.cancel_outlined,
    NotificationType.appointmentCancelled => Icons.event_busy,
    NotificationType.appointmentCompleted => Icons.task_alt,
    NotificationType.appointmentReminder => Icons.alarm,
    NotificationType.unknown => Icons.notifications_none,
  };
}

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final listState = ref.watch(notificationsControllerProvider);
    final controller = ref.read(notificationsControllerProvider.notifier);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabNotifications),
        actions: [
          IconButton(
            icon: const Icon(Icons.done_all),
            tooltip: l10n.markAllRead,
            onPressed: controller.markAllRead,
          ),
          ...sessionAppBarActions(context, ref),
        ],
      ),
      body: listState.when(
        loading: () => const SkeletonList(),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(notificationsControllerProvider),
        ),
        data: (data) {
          if (data.items.isEmpty) {
            return EmptyState(
              icon: Icons.notifications_none,
              title: l10n.noNotifications,
            );
          }
          return RefreshIndicator(
            onRefresh: () async =>
                ref.invalidate(notificationsControllerProvider),
            child: ListView.builder(
              itemCount: data.items.length + (data.isLast ? 0 : 1),
              itemBuilder: (context, index) {
                if (index >= data.items.length) {
                  Future.microtask(controller.loadMore);
                  return const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                final item = data.items[index];
                return ListTile(
                  leading: Icon(
                    notificationIcon(item.type),
                    color: item.read
                        ? null
                        : Theme.of(context).colorScheme.primary,
                  ),
                  title: Text(
                    item.message,
                    style: item.read
                        ? null
                        : const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  subtitle:
                      Text(formatRelativeTime(context, item.createdAt)),
                  onTap: item.read
                      ? null
                      : () => controller.markRead(item.id),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
