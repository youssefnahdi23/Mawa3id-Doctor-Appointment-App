import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/l10n.dart';
import '../data/appointment_models.dart';

String appointmentStatusLabel(
    AppLocalizations l10n, AppointmentStatus status) {
  return switch (status) {
    AppointmentStatus.pending => l10n.statusPending,
    AppointmentStatus.accepted => l10n.statusAccepted,
    AppointmentStatus.rejected => l10n.statusRejected,
    AppointmentStatus.cancelled => l10n.statusCancelled,
    AppointmentStatus.completed => l10n.statusCompleted,
  };
}

class AppointmentStatusChip extends StatelessWidget {
  const AppointmentStatusChip({super.key, required this.status});

  final AppointmentStatus status;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final (background, foreground) = switch (status) {
      AppointmentStatus.pending => (
          colors.tertiaryContainer,
          colors.onTertiaryContainer
        ),
      AppointmentStatus.accepted => (
          colors.primaryContainer,
          colors.onPrimaryContainer
        ),
      AppointmentStatus.completed => (
          colors.secondaryContainer,
          colors.onSecondaryContainer
        ),
      _ => (colors.errorContainer, colors.onErrorContainer),
    };
    return Chip(
      label: Text(appointmentStatusLabel(context.l10n, status)),
      backgroundColor: background,
      labelStyle: TextStyle(color: foreground, fontSize: 12),
      visualDensity: VisualDensity.compact,
      side: BorderSide.none,
    );
  }
}

/// `Wed, Jul 15 · 09:30–10:00` in the current locale, straight from
/// wall-clock values (no zone conversion).
String formatAppointmentRange(
    BuildContext context, DateTime start, DateTime end) {
  final locale = Localizations.localeOf(context).toString();
  final day = DateFormat.MMMEd(locale).format(start);
  final startTime = DateFormat.Hm(locale).format(start);
  final endTime = DateFormat.Hm(locale).format(end);
  return '$day · $startTime–$endTime';
}

/// Localized status filter chips shared by both appointment screens.
class StatusFilterChips extends StatelessWidget {
  const StatusFilterChips({
    super.key,
    required this.selected,
    required this.onChanged,
  });

  final AppointmentStatus? selected;
  final ValueChanged<AppointmentStatus?> onChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return SizedBox(
      height: 56,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        children: [
          FilterChip(
            label: Text(l10n.filterAll),
            selected: selected == null,
            onSelected: (_) => onChanged(null),
          ),
          for (final status in AppointmentStatus.values)
            Padding(
              padding: const EdgeInsetsDirectional.only(start: 8),
              child: FilterChip(
                label: Text(appointmentStatusLabel(l10n, status)),
                selected: selected == status,
                onSelected: (isSelected) =>
                    onChanged(isSelected ? status : null),
              ),
            ),
        ],
      ),
    );
  }
}
