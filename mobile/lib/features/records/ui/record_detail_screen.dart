import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/error_view.dart';
import '../state/records_providers.dart';
import 'record_widgets.dart';

/// Read-only medical record for one appointment (patient or doctor).
class RecordDetailScreen extends ConsumerWidget {
  const RecordDetailScreen({super.key, required this.appointmentId});

  final int appointmentId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final record = ref.watch(appointmentRecordProvider(appointmentId));
    return Scaffold(
      appBar: AppBar(title: Text(l10n.recordTitle)),
      body: record.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () =>
              ref.invalidate(appointmentRecordProvider(appointmentId)),
        ),
        data: (record) => record == null
            ? Center(child: Text(l10n.recordNoneYet))
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [RecordDetails(record: record)],
              ),
      ),
    );
  }
}
