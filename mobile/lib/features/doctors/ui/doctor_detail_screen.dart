import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/rating_stars.dart';
import '../data/doctor_models.dart';
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
      floatingActionButton: detail.hasValue
          ? FloatingActionButton.extended(
              onPressed: () => context.go('/p/doctors/$doctorId/book'),
              icon: const Icon(Icons.event_available),
              label: Text(l10n.bookAppointment),
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
    final reviews = ref.watch(doctorReviewsProvider(doctor.userId));
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
      children: [
        Row(
          children: [
            CircleAvatar(
              radius: 28,
              child: Text(
                doctor.name.isEmpty ? '?' : doctor.name[0].toUpperCase(),
                style: const TextStyle(fontSize: 24),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(doctor.name,
                      style: Theme.of(context).textTheme.titleLarge),
                  if (doctor.specialty != null)
                    Text(doctor.specialty!.name,
                        style: Theme.of(context).textTheme.bodyMedium),
                  RatingStars(
                    average: doctor.ratingAverage,
                    count: doctor.ratingCount,
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Icon(
                  doctor.acceptanceMode == AcceptanceMode.auto
                      ? Icons.flash_on
                      : Icons.hourglass_top,
                  color: Theme.of(context).colorScheme.primary,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    doctor.acceptanceMode == AcceptanceMode.auto
                        ? l10n.acceptanceAutoHint
                        : l10n.acceptanceManualHint,
                  ),
                ),
              ],
            ),
          ),
        ),
        if (doctor.bio != null && doctor.bio!.isNotEmpty) ...[
          const SizedBox(height: 16),
          Text(doctor.bio!),
        ],
        const SizedBox(height: 16),
        if (doctor.cabinetAddress != null &&
            doctor.cabinetAddress!.isNotEmpty)
          _InfoRow(icon: Icons.place_outlined, text: doctor.cabinetAddress!),
        if (doctor.phone != null && doctor.phone!.isNotEmpty)
          _InfoRow(icon: Icons.phone_outlined, text: doctor.phone!),
        if (doctor.workingHours != null && doctor.workingHours!.isNotEmpty)
          _InfoRow(
              icon: Icons.access_time_outlined, text: doctor.workingHours!),
        const SizedBox(height: 24),
        Text(l10n.reviewsTitle,
            style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        reviews.when(
          loading: () => const Padding(
            padding: EdgeInsets.all(16),
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (_, __) => Text(l10n.errorGeneric),
          data: (page) => page.content.isEmpty
              ? Text(l10n.noReviewsYet)
              : Column(
                  children: [
                    for (final review in page.content)
                      Card(
                        child: ListTile(
                          title: Row(
                            children: [
                              Expanded(child: Text(review.patientName)),
                              RatingStars(
                                average: review.rating.toDouble(),
                                size: 14,
                              ),
                            ],
                          ),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if (review.comment != null &&
                                  review.comment!.isNotEmpty)
                                Text(review.comment!),
                              Text(
                                DateFormat.yMMMd(
                                        Localizations.localeOf(context)
                                            .toString())
                                    .format(review.createdAt.toLocal()),
                                style:
                                    Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
        ),
      ],
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
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Icon(icon, size: 20),
          const SizedBox(width: 12),
          Expanded(child: Text(text)),
        ],
      ),
    );
  }
}
