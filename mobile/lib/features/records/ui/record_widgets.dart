import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/l10n.dart';
import '../data/record_models.dart';

/// Read-only view of a single medical record's fields, shared by the record
/// detail screen and the patient history list.
class RecordDetails extends StatelessWidget {
  const RecordDetails({super.key, required this.record, this.showVisit = true});

  final MedicalRecordResponse record;

  /// Whether to show the visit date/doctor header (hidden when the surrounding
  /// card already shows it).
  final bool showVisit;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final locale = Localizations.localeOf(context).toString();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (showVisit) ...[
          Text(record.doctorName,
              style: Theme.of(context).textTheme.titleMedium),
          Text(
            DateFormat.yMMMMEEEEd(locale).add_Hm().format(record.visitStart),
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const Divider(height: 24),
        ],
        _Field(label: l10n.recordDiagnosis, value: record.diagnosis),
        if (record.notes != null && record.notes!.isNotEmpty)
          _Field(label: l10n.recordNotes, value: record.notes!),
        if (record.prescription != null && record.prescription!.isNotEmpty)
          _Field(label: l10n.recordPrescription, value: record.prescription!),
        if (record.followUpDate != null)
          _Field(
            label: l10n.recordFollowUp,
            value: DateFormat.yMMMMd(locale).format(record.followUpDate!),
          ),
      ],
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: Theme.of(context).colorScheme.primary)),
          const SizedBox(height: 2),
          Text(value, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}
