import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/app_components.dart';
import '../../../core/widgets/error_view.dart';
import '../../appointments/data/appointment_models.dart';
import '../../appointments/state/appointments_providers.dart';
import '../../appointments/ui/appointment_widgets.dart';
import '../../auth/state/session_controller.dart';
import '../../notifications/state/unread_count_controller.dart';
import '../state/patient_providers.dart';

/// The patient's profile overview: identity header, next appointment, and an
/// account-settings list (edit profile, medical history, notifications, log
/// out). Editing the profile lives on a dedicated sub-screen.
class PatientProfileScreen extends ConsumerWidget {
  const PatientProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final profile = ref.watch(myPatientProfileProvider);
    final appointments = ref.watch(patientAppointmentsProvider);
    final unread = ref.watch(unreadCountProvider).valueOrNull ?? 0;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.tabProfile)),
      body: profile.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(myPatientProfileProvider),
        ),
        data: (patient) {
          final upcoming = _nextAppointment(appointments.valueOrNull?.items);
          return ListView(
            padding: const EdgeInsets.fromLTRB(
                AppSpacing.lg, AppSpacing.lg, AppSpacing.lg, AppSpacing.xxl),
            children: [
              // ── Identity header ──
              Center(
                child: Column(
                  children: [
                    AppAvatar(name: patient.fullName, radius: 44),
                    const SizedBox(height: AppSpacing.md),
                    Text(patient.fullName,
                        style: Theme.of(context).textTheme.headlineSmall),
                    const SizedBox(height: AppSpacing.xs),
                    TagBadge(
                      label: '${l10n.patientCodeLabel} · ${patient.patientCode}',
                      tone: BadgeTone.secondary,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.xl),

              // ── Next appointment ──
              SectionHeader(
                title: l10n.profileMyAppointments,
                actionLabel: l10n.actionSeeAll,
                onAction: () => context.go('/p/appointments'),
              ),
              const SizedBox(height: AppSpacing.md),
              if (upcoming != null)
                _AppointmentPreview(appointment: upcoming)
              else
                _Hint(text: l10n.homeNoUpcoming),
              const SizedBox(height: AppSpacing.xl),

              // ── Account settings ──
              Text(l10n.profileAccountSettings,
                  style: Theme.of(context)
                      .textTheme
                      .titleMedium
                      ?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: AppSpacing.sm),
              _SettingsTile(
                icon: Icons.person_outline,
                title: l10n.profileEditProfile,
                subtitle: l10n.profileEditProfileSubtitle,
                onTap: () => context.go('/p/profile/edit'),
              ),
              _SettingsTile(
                icon: Icons.medical_information_outlined,
                title: l10n.profileMedicalHistory,
                subtitle: l10n.profileMedicalHistorySubtitle,
                onTap: () => context.go('/p/profile/records'),
              ),
              _SettingsTile(
                icon: Icons.notifications_outlined,
                title: l10n.tabNotifications,
                subtitle: l10n.profileNotificationsSubtitle,
                trailingBadge: unread > 0 ? (unread > 99 ? '99+' : '$unread') : null,
                onTap: () => context.go('/p/profile/notifications'),
              ),
              _SettingsTile(
                icon: Icons.logout,
                title: l10n.logout,
                destructive: true,
                onTap: () => _confirmLogout(context, ref),
              ),
            ],
          );
        },
      ),
    );
  }

  AppointmentResponse? _nextAppointment(List<AppointmentResponse>? items) {
    if (items == null) return null;
    final now = DateTime.now();
    final active = items
        .where((a) =>
            (a.status == AppointmentStatus.pending ||
                a.status == AppointmentStatus.accepted) &&
            a.end.isAfter(now))
        .toList()
      ..sort((a, b) => a.start.compareTo(b.start));
    return active.isEmpty ? null : active.first;
  }

  Future<void> _confirmLogout(BuildContext context, WidgetRef ref) async {
    final l10n = context.l10n;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.logout),
        content: Text(l10n.logoutConfirm),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(l10n.actionCancel)),
          FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(l10n.logout)),
        ],
      ),
    );
    if (ok == true) {
      await ref.read(sessionControllerProvider.notifier).logout();
    }
  }
}

/// Compact card for the next appointment with Reschedule / Details actions.
class _AppointmentPreview extends StatelessWidget {
  const _AppointmentPreview({required this.appointment});

  final AppointmentResponse appointment;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerLow,
        borderRadius: AppRadii.cardR,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(appointment.doctorName,
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w700)),
              ),
              AppointmentStatusChip(status: appointment.status),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              Icon(Icons.calendar_today_outlined,
                  size: 16, color: theme.colorScheme.onSurfaceVariant),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Text(
                  formatAppointmentRange(
                      context, appointment.start, appointment.end),
                  style: theme.textTheme.bodyMedium,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => context.go(
                      '/p/appointments/${appointment.id}/reschedule?doctorId=${appointment.doctorId}'),
                  child: Text(l10n.actionReschedule),
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: FilledButton(
                  onPressed: () => context.go('/p/appointments'),
                  child: Text(l10n.detailsAction),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({
    required this.icon,
    required this.title,
    this.subtitle,
    this.trailingBadge,
    this.destructive = false,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String? subtitle;
  final String? trailingBadge;
  final bool destructive;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final color = destructive ? colors.error : colors.onSurface;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: destructive
              ? colors.errorContainer
              : colors.secondaryContainer,
          borderRadius: AppRadii.chipR,
        ),
        child: Icon(icon,
            size: 20,
            color: destructive
                ? colors.onErrorContainer
                : colors.onSecondaryContainer),
      ),
      title: Text(title,
          style: Theme.of(context)
              .textTheme
              .titleSmall
              ?.copyWith(fontWeight: FontWeight.w600, color: color)),
      subtitle: subtitle == null ? null : Text(subtitle!),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (trailingBadge != null)
            TagBadge(label: trailingBadge!, tone: BadgeTone.error),
          const SizedBox(width: AppSpacing.sm),
          Icon(Icons.chevron_right, color: colors.onSurfaceVariant),
        ],
      ),
      onTap: onTap,
    );
  }
}

class _Hint extends StatelessWidget {
  const _Hint({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: AppRadii.cardR,
      ),
      child: Text(text,
          style: Theme.of(context)
              .textTheme
              .bodyMedium
              ?.copyWith(color: colors.onSurfaceVariant)),
    );
  }
}
