import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/app_components.dart';
import '../../appointments/data/appointment_models.dart';
import '../../appointments/state/appointments_providers.dart';
import '../../doctors/data/doctor_models.dart';
import '../../doctors/state/doctors_providers.dart';
import '../../doctors/ui/doctor_card.dart';
import '../../patient/state/patient_providers.dart';

/// Patient landing dashboard: greeting, quick search, the next appointment,
/// specialty categories and a "Top doctors" rail. Everything is wired to the
/// existing providers; taps route into the Find / Bookings / Profile tabs.
class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final languageCode = Localizations.localeOf(context).languageCode;
    final profile = ref.watch(myPatientProfileProvider);
    final appointments = ref.watch(patientAppointmentsProvider);
    final specialties = ref.watch(specialtiesProvider);
    final doctors = ref.watch(doctorListControllerProvider);
    final specialtyById = {
      for (final s in specialties.valueOrNull ?? <SpecialtyResponse>[]) s.id: s,
    };

    final firstName = (profile.valueOrNull?.fullName.trim().isNotEmpty ?? false)
        ? profile.valueOrNull!.fullName.trim().split(' ').first
        : null;
    final upcoming = _nextAppointment(appointments.valueOrNull?.items);
    final topDoctors =
        (doctors.valueOrNull?.items ?? <DoctorSummary>[]).take(4).toList();

    return Scaffold(
      appBar: AppBar(
        titleSpacing: AppSpacing.lg,
        title: Text(
          'Mawa3id',
          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                color: Theme.of(context).colorScheme.primary,
                fontWeight: FontWeight.w700,
              ),
        ),
        actions: [
          Padding(
            padding: const EdgeInsetsDirectional.only(end: AppSpacing.lg),
            child: GestureDetector(
              onTap: () => context.go('/p/profile'),
              child: AppAvatar(name: profile.valueOrNull?.fullName ?? '', radius: 18),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.go('/p/doctors'),
        child: const Icon(Icons.add),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(myPatientProfileProvider);
          ref.invalidate(patientAppointmentsProvider);
          ref.invalidate(doctorListControllerProvider);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(
              AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.xxl),
          children: [
            Text(
              firstName == null
                  ? l10n.homeGreetingGeneric
                  : l10n.homeGreeting(firstName),
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              l10n.homeSubtitle,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: AppSpacing.lg),
            TextField(
              readOnly: true,
              onTap: () => context.go('/p/doctors'),
              decoration: InputDecoration(
                hintText: l10n.searchDoctorsHint,
                prefixIcon: const Icon(Icons.search),
              ),
            ),
            const SizedBox(height: AppSpacing.xl),

            SectionHeader(
              title: l10n.homeUpcomingTitle,
              actionLabel: l10n.actionSeeAll,
              onAction: () => context.go('/p/appointments'),
            ),
            const SizedBox(height: AppSpacing.md),
            if (upcoming != null)
              _UpcomingCard(
                appointment: upcoming,
                onTap: () => context.go('/p/appointments'),
              )
            else
              _EmptyHint(text: l10n.homeNoUpcoming),
            const SizedBox(height: AppSpacing.xl),

            if ((specialties.valueOrNull ?? []).isNotEmpty) ...[
              Text(
                l10n.homeCategoriesTitle,
                style: Theme.of(context)
                    .textTheme
                    .titleMedium
                    ?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: AppSpacing.md),
              SizedBox(
                height: 96,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: specialties.valueOrNull!.length,
                  separatorBuilder: (_, __) =>
                      const SizedBox(width: AppSpacing.md),
                  itemBuilder: (context, i) {
                    final s = specialties.valueOrNull![i];
                    return _CategoryChip(
                      icon: _specialtyIcon(s.name),
                      label: s.localizedName(languageCode),
                      onTap: () {
                        ref
                            .read(doctorListControllerProvider.notifier)
                            .setSpecialty(s.id);
                        context.go('/p/doctors');
                      },
                    );
                  },
                ),
              ),
              const SizedBox(height: AppSpacing.xl),
            ],

            SectionHeader(
              title: l10n.homeTopDoctorsTitle,
              actionLabel: l10n.actionViewAll,
              onAction: () => context.go('/p/doctors'),
            ),
            const SizedBox(height: AppSpacing.md),
            if (doctors.isLoading && topDoctors.isEmpty)
              const Center(
                  child: Padding(
                padding: EdgeInsets.all(AppSpacing.xl),
                child: CircularProgressIndicator(),
              ))
            else if (topDoctors.isEmpty)
              _EmptyHint(text: l10n.noDoctorsFound)
            else
              for (final d in topDoctors)
                Padding(
                  padding: const EdgeInsets.only(bottom: AppSpacing.md),
                  child: DoctorCard(
                    doctor: d,
                    specialtyLabel: d.specialtyId != null
                        ? specialtyById[d.specialtyId]
                                ?.localizedName(languageCode) ??
                            d.specialtyName
                        : d.specialtyName,
                    onTap: () => context.go('/p/doctors/${d.userId}'),
                  ),
                ),
          ],
        ),
      ),
    );
  }

  /// The soonest still-active (pending/accepted) appointment that hasn't ended.
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
}

IconData _specialtyIcon(String englishName) {
  final n = englishName.toLowerCase();
  if (n.contains('cardio') || n.contains('heart')) return Icons.favorite_outline;
  if (n.contains('dent') || n.contains('oral')) return Icons.masks_outlined;
  if (n.contains('psych') || n.contains('mental') || n.contains('neuro')) {
    return Icons.psychology_outlined;
  }
  if (n.contains('eye') || n.contains('ophtha')) {
    return Icons.remove_red_eye_outlined;
  }
  if (n.contains('pharm')) return Icons.local_pharmacy_outlined;
  if (n.contains('pedia') || n.contains('child')) {
    return Icons.child_care_outlined;
  }
  if (n.contains('derma') || n.contains('skin')) return Icons.spa_outlined;
  if (n.contains('gyne') || n.contains('obste')) {
    return Icons.pregnant_woman_outlined;
  }
  if (n.contains('ortho') || n.contains('bone')) {
    return Icons.accessible_outlined;
  }
  return Icons.medical_services_outlined;
}

/// Green "next appointment" hero card.
class _UpcomingCard extends StatelessWidget {
  const _UpcomingCard({required this.appointment, required this.onTap});

  final AppointmentResponse appointment;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final locale = Localizations.localeOf(context).toString();
    final when =
        '${DateFormat.MMMEd(locale).format(appointment.start)} · ${DateFormat.Hm(locale).format(appointment.start)}';
    return Material(
      color: colors.primary,
      borderRadius: AppRadii.cardR,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Row(
            children: [
              CircleAvatar(
                radius: 24,
                backgroundColor: colors.onPrimary.withValues(alpha: 0.2),
                child: Icon(Icons.person, color: colors.onPrimary),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      appointment.doctorName,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: colors.onPrimary, fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(Icons.calendar_today,
                            size: 14, color: colors.onPrimary),
                        const SizedBox(width: 6),
                        Text(
                          when,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: colors.onPrimary.withValues(alpha: 0.9)),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Icon(Icons.videocam_outlined, color: colors.onPrimary),
            ],
          ),
        ),
      ),
    );
  }
}

class _CategoryChip extends StatelessWidget {
  const _CategoryChip(
      {required this.icon, required this.label, required this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      borderRadius: AppRadii.cardR,
      child: SizedBox(
        width: 76,
        child: Column(
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: colors.secondaryContainer,
                borderRadius: AppRadii.cardR,
              ),
              child: Icon(icon, color: colors.onSecondaryContainer),
            ),
            const SizedBox(height: 6),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.labelMedium,
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyHint extends StatelessWidget {
  const _EmptyHint({required this.text});

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
      child: Text(
        text,
        style: Theme.of(context)
            .textTheme
            .bodyMedium
            ?.copyWith(color: colors.onSurfaceVariant),
      ),
    );
  }
}
