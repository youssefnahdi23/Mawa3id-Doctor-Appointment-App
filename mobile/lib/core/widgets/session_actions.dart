import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/state/session_controller.dart';
import '../l10n/l10n.dart';
import '../l10n/locale_controller.dart';
import '../theme/theme_controller.dart';

/// AppBar actions shared by every signed-in screen: theme + language pickers,
/// account (contact verification), and logout.
List<Widget> sessionAppBarActions(BuildContext context, WidgetRef ref) => [
      const ThemeMenuButton(),
      const LocaleMenuButton(),
      const AccountMenuButton(),
      IconButton(
        icon: const Icon(Icons.logout),
        tooltip: context.l10n.logout,
        onPressed: () =>
            ref.read(sessionControllerProvider.notifier).logout(),
      ),
    ];

/// Light / dark / system theme picker.
class ThemeMenuButton extends ConsumerWidget {
  const ThemeMenuButton({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final current = ref.watch(themeControllerProvider);
    final l10n = context.l10n;
    final choices = <(ThemeMode, String, IconData)>[
      (ThemeMode.system, l10n.themeSystem, Icons.brightness_auto_outlined),
      (ThemeMode.light, l10n.themeLight, Icons.light_mode_outlined),
      (ThemeMode.dark, l10n.themeDark, Icons.dark_mode_outlined),
    ];
    return PopupMenuButton<ThemeMode>(
      icon: const Icon(Icons.brightness_6_outlined),
      tooltip: l10n.theme,
      onSelected: (mode) =>
          ref.read(themeControllerProvider.notifier).set(mode),
      itemBuilder: (context) => [
        for (final (mode, label, icon) in choices)
          CheckedPopupMenuItem(
            value: mode,
            checked: current == mode,
            child: Row(
              children: [
                Icon(icon, size: 20),
                const SizedBox(width: 12),
                Text(label),
              ],
            ),
          ),
      ],
    );
  }
}

/// Account menu: entry points to verify the user's email and phone (with their
/// current verification state), and a "sign out of all devices" action.
class AccountMenuButton extends ConsumerWidget {
  const AccountMenuButton({super.key});

  static const _logoutAll = '__logout_all__';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final session = ref.watch(sessionControllerProvider).valueOrNull;
    return PopupMenuButton<String>(
      icon: const Icon(Icons.account_circle_outlined),
      tooltip: l10n.account,
      onSelected: (value) {
        if (value == _logoutAll) {
          _confirmLogoutAll(context, ref);
        } else {
          context.push(value);
        }
      },
      itemBuilder: (context) => [
        PopupMenuItem(
          value: '/verify/email',
          child: _VerifyMenuRow(
            label: l10n.verifyEmailTitle,
            verified: session?.emailVerified ?? false,
          ),
        ),
        PopupMenuItem(
          value: '/verify/phone',
          child: _VerifyMenuRow(
            label: l10n.verifyPhoneTitle,
            verified: session?.phoneVerified ?? false,
          ),
        ),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: _logoutAll,
          child: Text(l10n.logoutAllTitle),
        ),
      ],
    );
  }

  Future<void> _confirmLogoutAll(BuildContext context, WidgetRef ref) async {
    final l10n = context.l10n;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.logoutAllTitle),
        content: Text(l10n.logoutAllConfirm),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l10n.actionCancel),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(l10n.logoutAllConfirmAction),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await ref.read(sessionControllerProvider.notifier).logoutAll();
    }
  }
}

/// A verify-menu row: the action label with a trailing verified / not-verified
/// status chip driven by the session.
class _VerifyMenuRow extends StatelessWidget {
  const _VerifyMenuRow({required this.label, required this.verified});

  final String label;
  final bool verified;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final colors = Theme.of(context).colorScheme;
    final color = verified ? Colors.green : colors.error;
    return Row(
      children: [
        Expanded(child: Text(label)),
        const SizedBox(width: 12),
        Icon(verified ? Icons.verified : Icons.error_outline,
            size: 16, color: color),
        const SizedBox(width: 4),
        Text(
          verified ? l10n.statusVerified : l10n.statusNotVerified,
          style: TextStyle(fontSize: 12, color: color),
        ),
      ],
    );
  }
}

/// Language picker; `null` follows the device locale.
class LocaleMenuButton extends ConsumerWidget {
  const LocaleMenuButton({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final current = ref.watch(localeControllerProvider);
    final l10n = context.l10n;
    final choices = <(Locale?, String)>[
      (null, l10n.languageSystem),
      (const Locale('en'), 'English'),
      (const Locale('fr'), 'Français'),
      (const Locale('ar'), 'العربية'),
    ];
    return PopupMenuButton<int>(
      icon: const Icon(Icons.language),
      tooltip: l10n.language,
      onSelected: (index) =>
          ref.read(localeControllerProvider.notifier).set(choices[index].$1),
      itemBuilder: (context) => [
        for (var i = 0; i < choices.length; i++)
          CheckedPopupMenuItem(
            value: i,
            checked: current?.languageCode == choices[i].$1?.languageCode,
            child: Text(choices[i].$2),
          ),
      ],
    );
  }
}
