import 'package:flutter/material.dart';

import '../theme/app_tokens.dart';

/// Section title with an optional trailing "See all" style action, used on the
/// Home dashboard and profile ("My Appointments", "Top Doctors", …).
class SectionHeader extends StatelessWidget {
  const SectionHeader({
    super.key,
    required this.title,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: theme.textTheme.titleMedium
                ?.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        if (actionLabel != null && onAction != null)
          TextButton(
            onPressed: onAction,
            style: TextButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
              minimumSize: const Size(0, 0),
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: Text(actionLabel!),
          ),
      ],
    );
  }
}

/// Circular avatar showing an initial on a tonal primary background. Falls back
/// to a person glyph when there's no name. (Image support can layer in later.)
class AppAvatar extends StatelessWidget {
  const AppAvatar({super.key, required this.name, this.radius = 24});

  final String name;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final initial = name.trim().isEmpty ? '' : name.trim()[0].toUpperCase();
    return CircleAvatar(
      radius: radius,
      backgroundColor: colors.primaryContainer,
      foregroundColor: colors.onPrimaryContainer,
      child: initial.isEmpty
          ? Icon(Icons.person, size: radius, color: colors.onPrimaryContainer)
          : Text(
              initial,
              style: TextStyle(
                fontSize: radius * 0.8,
                fontWeight: FontWeight.w700,
                color: colors.onPrimaryContainer,
              ),
            ),
    );
  }
}

/// Tonal colour intents for a [TagBadge].
enum BadgeTone { primary, secondary, tertiary, neutral, success, error }

/// Compact, tonal-filled metadata label (6px radius) — CNAM conventionné,
/// spoken languages, "Top rated", availability, patient IDs, etc.
class TagBadge extends StatelessWidget {
  const TagBadge({
    super.key,
    required this.label,
    this.tone = BadgeTone.neutral,
    this.icon,
  });

  final String label;
  final BadgeTone tone;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final (bg, fg) = switch (tone) {
      BadgeTone.primary => (colors.primaryContainer, colors.onPrimaryContainer),
      BadgeTone.secondary => (
          colors.secondaryContainer,
          colors.onSecondaryContainer
        ),
      BadgeTone.tertiary => (
          colors.tertiaryContainer,
          colors.onTertiaryContainer
        ),
      BadgeTone.success => (colors.primaryContainer, colors.onPrimaryContainer),
      BadgeTone.error => (colors.errorContainer, colors.onErrorContainer),
      BadgeTone.neutral => (
          colors.surfaceContainerHighest,
          colors.onSurfaceVariant
        ),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: bg, borderRadius: AppRadii.badgeR),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 12, color: fg),
            const SizedBox(width: 4),
          ],
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: fg,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.2,
                ),
          ),
        ],
      ),
    );
  }
}

/// A small green "★ 4.9" rating pill shown on doctor cards/headers.
class RatingBadge extends StatelessWidget {
  const RatingBadge({super.key, required this.average, this.count});

  final double average;
  final int? count;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: colors.primaryContainer,
        borderRadius: AppRadii.badgeR,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.star_rounded, size: 14, color: colors.onPrimaryContainer),
          const SizedBox(width: 2),
          Text(
            average.toStringAsFixed(1),
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: colors.onPrimaryContainer,
                  fontWeight: FontWeight.w700,
                ),
          ),
        ],
      ),
    );
  }
}

/// A single "value over label" statistic, three-up on the doctor detail hero
/// (Exp years / Patients / Rating).
class StatTile extends StatelessWidget {
  const StatTile({super.key, required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      children: [
        Text(
          value,
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w700,
            color: theme.colorScheme.primary,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          textAlign: TextAlign.center,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}
