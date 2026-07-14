import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/state/session_controller.dart';
import '../l10n/l10n.dart';
import '../l10n/locale_controller.dart';

/// AppBar actions shared by every signed-in screen: language picker + logout.
List<Widget> sessionAppBarActions(BuildContext context, WidgetRef ref) => [
      const LocaleMenuButton(),
      IconButton(
        icon: const Icon(Icons.logout),
        tooltip: context.l10n.logout,
        onPressed: () =>
            ref.read(sessionControllerProvider.notifier).logout(),
      ),
    ];

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
