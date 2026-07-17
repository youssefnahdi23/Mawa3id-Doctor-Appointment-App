import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/state/session_controller.dart';
import '../l10n/l10n.dart';
import '../l10n/locale_controller.dart';

/// AppBar actions shared by every signed-in screen: language picker, account
/// (contact verification), and logout.
List<Widget> sessionAppBarActions(BuildContext context, WidgetRef ref) => [
      const LocaleMenuButton(),
      const AccountMenuButton(),
      IconButton(
        icon: const Icon(Icons.logout),
        tooltip: context.l10n.logout,
        onPressed: () =>
            ref.read(sessionControllerProvider.notifier).logout(),
      ),
    ];

/// Account menu: entry points to verify the user's email and phone.
class AccountMenuButton extends StatelessWidget {
  const AccountMenuButton({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return PopupMenuButton<String>(
      icon: const Icon(Icons.account_circle_outlined),
      tooltip: l10n.account,
      onSelected: (route) => context.push(route),
      itemBuilder: (context) => [
        PopupMenuItem(value: '/verify/email', child: Text(l10n.verifyEmailTitle)),
        PopupMenuItem(value: '/verify/phone', child: Text(l10n.verifyPhoneTitle)),
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
