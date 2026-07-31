import 'package:flutter/material.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/app_components.dart';
import '../data/doctor_models.dart';
import '../data/doctor_reference.dart';

/// Rich doctor card shared by the Find list and the Home "Top doctors" rail:
/// avatar, name, specialty, location, a rating pill and tonal metadata badges
/// (CNAM conventionné, fee, "Top rated").
class DoctorCard extends StatelessWidget {
  const DoctorCard({
    super.key,
    required this.doctor,
    required this.specialtyLabel,
    required this.onTap,
  });

  final DoctorSummary doctor;
  final String? specialtyLabel;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final theme = Theme.of(context);
    final govLabel = governorateLabel(l10n, doctor.governorate);
    final topRated = doctor.ratingCount > 0 && doctor.ratingAverage >= 4.8;

    final badges = <Widget>[
      if (doctor.cnamConventionne)
        TagBadge(label: l10n.cnamBadge, tone: BadgeTone.primary),
      if (topRated) TagBadge(label: l10n.tagTopRated, tone: BadgeTone.tertiary),
      if (doctor.consultationFee != null)
        TagBadge(
          label: '${doctor.consultationFee} ${l10n.consultationFeeUnit}',
          tone: BadgeTone.neutral,
        ),
    ];

    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: AppRadii.cardR,
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AppAvatar(name: doctor.name, radius: 28),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Text(
                            doctor.name,
                            style: theme.textTheme.titleMedium
                                ?.copyWith(fontWeight: FontWeight.w700),
                          ),
                        ),
                        if (doctor.ratingCount > 0)
                          RatingBadge(average: doctor.ratingAverage),
                      ],
                    ),
                    if (specialtyLabel != null)
                      Text(
                        specialtyLabel!,
                        style: theme.textTheme.bodyMedium
                            ?.copyWith(color: theme.colorScheme.primary),
                      ),
                    if (govLabel.isNotEmpty) ...[
                      const SizedBox(height: 2),
                      Row(
                        children: [
                          Icon(Icons.location_on_outlined,
                              size: 14, color: theme.colorScheme.onSurfaceVariant),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              govLabel,
                              style: theme.textTheme.bodySmall?.copyWith(
                                  color: theme.colorScheme.onSurfaceVariant),
                            ),
                          ),
                        ],
                      ),
                    ],
                    if (badges.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.sm),
                      Wrap(
                        spacing: AppSpacing.sm,
                        runSpacing: AppSpacing.xs,
                        children: badges,
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
