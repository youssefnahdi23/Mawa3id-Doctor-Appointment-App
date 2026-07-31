import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/app_components.dart';
import '../../../core/widgets/error_view.dart';
import '../../availability/data/availability_models.dart';
import '../data/doctor_models.dart';
import '../data/doctor_reference.dart';
import '../state/doctors_providers.dart';

class DoctorDetailScreen extends ConsumerWidget {
  const DoctorDetailScreen({super.key, required this.doctorId});

  final int doctorId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final detail = ref.watch(doctorDetailProvider(doctorId));
    return Scaffold(
      appBar: AppBar(title: Text(l10n.doctorDetailTitle)),
      body: detail.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(doctorDetailProvider(doctorId)),
        ),
        data: (doctor) => _DoctorDetailBody(doctor: doctor),
      ),
      bottomNavigationBar: detail.hasValue
          ? SafeArea(
              minimum: const EdgeInsets.fromLTRB(
                  AppSpacing.lg, 0, AppSpacing.lg, AppSpacing.md),
              child: FilledButton.icon(
                onPressed: () => context.go('/p/doctors/$doctorId/book'),
                icon: const Icon(Icons.event_available_outlined),
                label: Text(l10n.bookAppointment),
              ),
            )
          : null,
    );
  }
}

class _DoctorDetailBody extends ConsumerWidget {
  const _DoctorDetailBody({required this.doctor});

  final DoctorResponse doctor;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final theme = Theme.of(context);
    final languageCode = Localizations.localeOf(context).languageCode;
    final reviews = ref.watch(doctorReviewsProvider(doctor.userId));
    final topRated = doctor.ratingCount > 0 && doctor.ratingAverage >= 4.8;

    return ListView(
      padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg, AppSpacing.lg, AppSpacing.lg, AppSpacing.xxl),
      children: [
        // ── Hero ──
        Center(
          child: Column(
            children: [
              AppAvatar(name: doctor.name, radius: 44),
              const SizedBox(height: AppSpacing.md),
              Wrap(
                spacing: AppSpacing.sm,
                children: [
                  if (doctor.cnamConventionne)
                    TagBadge(
                        label: l10n.cnamConventionneLabel,
                        tone: BadgeTone.primary),
                  if (topRated)
                    TagBadge(label: l10n.tagTopRated, tone: BadgeTone.tertiary),
                ],
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(doctor.name,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.headlineSmall),
              if (doctor.specialty != null)
                Text(
                  doctor.specialty!.localizedName(languageCode).toUpperCase(),
                  textAlign: TextAlign.center,
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.primary,
                    letterSpacing: 0.6,
                  ),
                ),
              if (governorateLabel(l10n, doctor.governorate).isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.location_on_outlined,
                        size: 16, color: theme.colorScheme.onSurfaceVariant),
                    const SizedBox(width: 4),
                    Text(governorateLabel(l10n, doctor.governorate),
                        style: theme.textTheme.bodyMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.lg),

        // ── Stats ──
        Container(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainerLow,
            borderRadius: AppRadii.cardR,
          ),
          child: Row(
            children: [
              Expanded(
                child: StatTile(
                  value: doctor.ratingAverage.toStringAsFixed(1),
                  label: l10n.statRating,
                ),
              ),
              _StatDivider(),
              Expanded(
                child: StatTile(
                  value: '${doctor.ratingCount}',
                  label: l10n.statReviews,
                ),
              ),
              _StatDivider(),
              Expanded(
                child: StatTile(
                  value: doctor.consultationFee != null
                      ? '${doctor.consultationFee}'
                      : '—',
                  label: l10n.consultationFeeUnit,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.lg),

        // ── Acceptance mode ──
        Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: BoxDecoration(
            color: theme.colorScheme.secondaryContainer,
            borderRadius: AppRadii.cardR,
          ),
          child: Row(
            children: [
              Icon(
                doctor.acceptanceMode == AcceptanceMode.auto
                    ? Icons.flash_on
                    : Icons.hourglass_top,
                color: theme.colorScheme.onSecondaryContainer,
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Text(
                  doctor.acceptanceMode == AcceptanceMode.auto
                      ? l10n.acceptanceAutoHint
                      : l10n.acceptanceManualHint,
                  style: TextStyle(
                      color: theme.colorScheme.onSecondaryContainer),
                ),
              ),
            ],
          ),
        ),

        if (doctor.bio != null && doctor.bio!.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.xl),
          Text(l10n.aboutDoctor,
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: AppSpacing.sm),
          Text(doctor.bio!,
              style: theme.textTheme.bodyMedium?.copyWith(height: 1.5)),
        ],

        const SizedBox(height: AppSpacing.xl),
        _InfoSection(doctor: doctor),

        const SizedBox(height: AppSpacing.xl),
        _ConsultationHours(doctorId: doctor.userId),

        Text(l10n.reviewsTitle,
            style: theme.textTheme.titleMedium
                ?.copyWith(fontWeight: FontWeight.w700)),
        const SizedBox(height: AppSpacing.sm),
        reviews.when(
          loading: () => const Padding(
            padding: EdgeInsets.all(AppSpacing.lg),
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (_, __) => Text(l10n.errorGeneric),
          data: (page) => page.content.isEmpty
              ? Text(l10n.noReviewsYet,
                  style: TextStyle(color: theme.colorScheme.onSurfaceVariant))
              : Column(
                  children: [
                    for (final review in page.content)
                      Padding(
                        padding:
                            const EdgeInsets.only(bottom: AppSpacing.md),
                        child: _ReviewTile(review: review),
                      ),
                  ],
                ),
        ),
      ],
    );
  }
}

class _StatDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) => Container(
        width: 1,
        height: 32,
        color: Theme.of(context).colorScheme.outlineVariant,
      );
}

class _InfoSection extends StatelessWidget {
  const _InfoSection({required this.doctor});

  final DoctorResponse doctor;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final rows = <Widget>[
      if (doctor.cabinetAddress != null && doctor.cabinetAddress!.isNotEmpty)
        _InfoRow(icon: Icons.place_outlined, text: doctor.cabinetAddress!),
      if (doctor.consultationFee != null)
        _InfoRow(
            icon: Icons.payments_outlined,
            text: '${doctor.consultationFee} ${l10n.consultationFeeUnit}'),
      if (doctor.languages.isNotEmpty)
        _InfoRow(
            icon: Icons.translate_outlined,
            text: doctor.languages
                .map((code) => languageLabel(l10n, code))
                .join(' · ')),
      if (doctor.phone != null && doctor.phone!.isNotEmpty)
        _InfoRow(icon: Icons.phone_outlined, text: doctor.phone!),
      if (doctor.workingHours != null && doctor.workingHours!.isNotEmpty)
        _InfoRow(icon: Icons.access_time_outlined, text: doctor.workingHours!),
    ];
    if (rows.isEmpty) return const SizedBox.shrink();
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerLow,
        borderRadius: AppRadii.cardR,
      ),
      child: Column(children: rows),
    );
  }
}

class _ReviewTile extends StatelessWidget {
  const _ReviewTile({required this.review});

  final ReviewResponse review;

  @override
  Widget build(BuildContext context) {
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
                child: Text(review.patientName,
                    style: theme.textTheme.titleSmall
                        ?.copyWith(fontWeight: FontWeight.w600)),
              ),
              RatingBadge(average: review.rating.toDouble()),
            ],
          ),
          if (review.comment != null && review.comment!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(review.comment!, style: theme.textTheme.bodyMedium),
          ],
          const SizedBox(height: 4),
          Text(
            DateFormat.yMMMd(Localizations.localeOf(context).toString())
                .format(review.createdAt.toLocal()),
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: AppSpacing.md),
          Expanded(child: Text(text)),
        ],
      ),
    );
  }
}

/// Weekly consultation hours with a per-window walk-in vs "By appointment only"
/// (RDVs Only) badge, so patients know which days they can just show up.
class _ConsultationHours extends ConsumerWidget {
  const _ConsultationHours({required this.doctorId});

  final int doctorId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final theme = Theme.of(context);
    final rules = ref.watch(doctorAvailabilityProvider(doctorId));
    return rules.maybeWhen(
      orElse: () => const SizedBox.shrink(),
      data: (list) {
        if (list.isEmpty) return const SizedBox.shrink();
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.consultationHours,
                style: theme.textTheme.titleMedium
                    ?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: AppSpacing.sm),
            Container(
              padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md, vertical: AppSpacing.xs),
              decoration: BoxDecoration(
                color: theme.colorScheme.surfaceContainerLow,
                borderRadius: AppRadii.cardR,
              ),
              child: Column(
                children: [
                  for (final rule in list)
                    Padding(
                      padding: const EdgeInsets.symmetric(
                          vertical: AppSpacing.sm),
                      child: Row(
                        children: [
                          SizedBox(
                            width: 96,
                            child: Text(_weekdayName(context, rule.dayOfWeek),
                                style: theme.textTheme.titleSmall?.copyWith(
                                    fontWeight: FontWeight.w600)),
                          ),
                          Expanded(
                            child: Text(
                                '${_hhmm(rule.startTime)}–${_hhmm(rule.endTime)}',
                                style: theme.textTheme.bodyMedium),
                          ),
                          TagBadge(
                            label: rule.rdvOnly
                                ? l10n.rdvOnlyLabel
                                : l10n.walkInWelcome,
                            tone: rule.rdvOnly
                                ? BadgeTone.primary
                                : BadgeTone.neutral,
                            icon: rule.rdvOnly
                                ? Icons.event_available
                                : Icons.directions_walk,
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
          ],
        );
      },
    );
  }
}

/// Localized weekday name for the availability `Weekday` enum (Monday-first).
String _weekdayName(BuildContext context, Weekday day) {
  final locale = Localizations.localeOf(context).toString();
  // 2024-01-01 is a Monday; offset by the enum's Monday-first index.
  return DateFormat.EEEE(locale).format(DateTime(2024, 1, 1 + day.index));
}

/// Trims a `HH:mm[:ss]` time string to `HH:mm` for display.
String _hhmm(String time) => time.length >= 5 ? time.substring(0, 5) : time;
