import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/empty_state.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/loading_skeleton.dart';
import '../state/records_providers.dart';
import 'record_widgets.dart';

/// The patient's medical record history (`GET /api/records/me`), newest first.
class PatientRecordsScreen extends ConsumerWidget {
  const PatientRecordsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final listState = ref.watch(patientRecordsProvider);
    final controller = ref.read(patientRecordsProvider.notifier);
    return Scaffold(
      appBar: AppBar(title: Text(l10n.myRecordsTitle)),
      body: listState.when(
        loading: () => const SkeletonList(),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(patientRecordsProvider),
        ),
        data: (data) {
          if (data.items.isEmpty) {
            return EmptyState(
              icon: Icons.folder_open,
              title: l10n.myRecordsEmpty,
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(patientRecordsProvider),
            child: ListView.builder(
              itemCount: data.items.length + (data.isLast ? 0 : 1),
              itemBuilder: (context, index) {
                if (index >= data.items.length) {
                  Future.microtask(controller.loadMore);
                  return const Padding(
                    padding: EdgeInsets.all(16),
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                final record = data.items[index];
                return Card(
                  margin: const EdgeInsets.symmetric(
                      horizontal: 16, vertical: 6),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: RecordDetails(record: record),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
