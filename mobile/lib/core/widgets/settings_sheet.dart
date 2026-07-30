import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';

import '../l10n/l10n.dart';
import '../l10n/locale_controller.dart';
import '../theme/theme_controller.dart';

/// App version shown in the About dialog. Hardcoded because `package_info_plus`
/// is not a dependency; keep in sync with `version:` in pubspec.yaml.
const _appVersion = '1.0.0';

/// Settings entry point for the login screen: a gear button that opens a compact
/// modal bottom sheet with three rows — language, theme, and about. Reuses the
/// existing locale and theme controllers so the choices persist just like the
/// signed-in menus.
class SettingsButton extends StatelessWidget {
  const SettingsButton({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return IconButton(
      icon: const Icon(Icons.settings_outlined),
      tooltip: l10n.settings,
      onPressed: () => showModalBottomSheet<void>(
        context: context,
        showDragHandle: true,
        builder: (_) => const _SettingsSheet(),
      ),
    );
  }
}

class _SettingsSheet extends ConsumerWidget {
  const _SettingsSheet();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final locale = ref.watch(localeControllerProvider);
    final themeMode = ref.watch(themeControllerProvider);

    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: const Icon(Icons.language),
            title: Text(l10n.language),
            trailing: _TrailingValue(_localeLabel(l10n, locale)),
            onTap: () => _pickLanguage(context, ref, locale),
          ),
          ListTile(
            leading: const Icon(Icons.brightness_6_outlined),
            title: Text(l10n.theme),
            trailing: _TrailingValue(_themeLabel(l10n, themeMode)),
            onTap: () => _pickTheme(context, ref, themeMode),
          ),
          ListTile(
            leading: const Icon(Icons.groups_outlined),
            title: Text(l10n.communityTitle),
            onTap: () {
              // Capture the router before popping the sheet; the sheet's own
              // context is unmounted once it closes.
              final router = GoRouter.of(context);
              Navigator.of(context).pop();
              router.push('/community');
            },
          ),
          ListTile(
            leading: const Icon(Icons.info_outline),
            title: Text(l10n.about),
            onTap: () => _showAbout(context, l10n),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }

  // --- Selectors -----------------------------------------------------------

  Future<void> _pickLanguage(
      BuildContext context, WidgetRef ref, Locale? current) async {
    final l10n = context.l10n;
    final choices = <(Locale?, String)>[
      (null, l10n.languageSystem),
      (const Locale('en'), 'English'),
      (const Locale('fr'), 'Français'),
      (const Locale('ar'), 'العربية'),
    ];
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final (value, label) in choices)
              RadioListTile<String?>(
                value: value?.languageCode,
                // ignore: deprecated_member_use
                groupValue: current?.languageCode,
                // ignore: deprecated_member_use
                onChanged: (_) {
                  ref.read(localeControllerProvider.notifier).set(value);
                  Navigator.of(context).pop();
                },
                title: Text(label),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _pickTheme(
      BuildContext context, WidgetRef ref, ThemeMode current) async {
    final l10n = context.l10n;
    final choices = <(ThemeMode, String, IconData)>[
      (ThemeMode.system, l10n.themeSystem, Icons.brightness_auto_outlined),
      (ThemeMode.light, l10n.themeLight, Icons.light_mode_outlined),
      (ThemeMode.dark, l10n.themeDark, Icons.dark_mode_outlined),
    ];
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final (mode, label, icon) in choices)
              RadioListTile<ThemeMode>(
                value: mode,
                // ignore: deprecated_member_use
                groupValue: current,
                // ignore: deprecated_member_use
                onChanged: (_) {
                  ref.read(themeControllerProvider.notifier).set(mode);
                  Navigator.of(context).pop();
                },
                secondary: Icon(icon),
                title: Text(label),
              ),
          ],
        ),
      ),
    );
  }

  void _showAbout(BuildContext context, AppLocalizations l10n) {
    showAboutDialog(
      context: context,
      applicationName: l10n.appTitle,
      applicationVersion: _appVersion,
      applicationIcon: SvgPicture.asset(
        'assets/images/mawa3id_logo.svg',
        width: 48,
        height: 48,
      ),
      children: [Text(l10n.aboutAppDescription)],
    );
  }

  // --- Current-value labels ------------------------------------------------

  String _localeLabel(AppLocalizations l10n, Locale? locale) {
    switch (locale?.languageCode) {
      case 'en':
        return 'English';
      case 'fr':
        return 'Français';
      case 'ar':
        return 'العربية';
      default:
        return l10n.languageSystem;
    }
  }

  String _themeLabel(AppLocalizations l10n, ThemeMode mode) {
    switch (mode) {
      case ThemeMode.light:
        return l10n.themeLight;
      case ThemeMode.dark:
        return l10n.themeDark;
      case ThemeMode.system:
        return l10n.themeSystem;
    }
  }
}

/// The current-value label + chevron shown at the end of a settings row.
class _TrailingValue extends StatelessWidget {
  const _TrailingValue(this.value);

  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          value,
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(width: 4),
        Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
      ],
    );
  }
}
