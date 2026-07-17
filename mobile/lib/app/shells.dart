import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../core/l10n/l10n.dart';
import '../features/notifications/state/unread_count_controller.dart';

class ShellTab {
  const ShellTab({
    required this.icon,
    required this.label,
    this.showUnreadBadge = false,
  });

  final IconData icon;
  final String Function(AppLocalizations) label;
  final bool showUnreadBadge;
}

final patientTabs = [
  ShellTab(icon: Icons.search, label: (l10n) => l10n.tabDoctors),
  ShellTab(icon: Icons.event, label: (l10n) => l10n.tabAppointments),
  ShellTab(
      icon: Icons.notifications,
      label: (l10n) => l10n.tabNotifications,
      showUnreadBadge: true),
  ShellTab(icon: Icons.person, label: (l10n) => l10n.tabProfile),
];

final doctorTabs = [
  ShellTab(icon: Icons.event, label: (l10n) => l10n.tabAppointments),
  ShellTab(icon: Icons.schedule, label: (l10n) => l10n.tabAvailability),
  ShellTab(
      icon: Icons.notifications,
      label: (l10n) => l10n.tabNotifications,
      showUnreadBadge: true),
  ShellTab(icon: Icons.person, label: (l10n) => l10n.tabProfile),
];

/// Bottom-tab scaffold for both roles; only the tab set differs.
class RoleShell extends ConsumerWidget {
  const RoleShell({
    super.key,
    required this.navigationShell,
    required this.tabs,
  });

  final StatefulNavigationShell navigationShell;
  final List<ShellTab> tabs;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final unread = ref.watch(unreadCountProvider).valueOrNull ?? 0;
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: (index) {
          // Cheap staleness guard: recheck the badge on every tab switch.
          ref.read(unreadCountProvider.notifier).refresh();
          navigationShell.goBranch(
            index,
            initialLocation: index == navigationShell.currentIndex,
          );
        },
        destinations: [
          for (final tab in tabs)
            NavigationDestination(
              icon: tab.showUnreadBadge && unread > 0
                  ? Badge(
                      label: Text(unread > 99 ? '99+' : '$unread'),
                      child: Icon(tab.icon),
                    )
                  : Icon(tab.icon),
              label: tab.label(l10n),
            ),
        ],
      ),
    );
  }
}
