import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/appointment_models.dart';
import '../data/appointment_repository.dart';
import '../state/appointments_providers.dart';
import 'appointment_widgets.dart';

class PatientAppointmentsScreen extends ConsumerWidget {
  const PatientAppointmentsScreen({super.key});

  Future<void> _cancel(
      BuildContext context, WidgetRef ref, AppointmentResponse item) async {
    final l10n = context.l10n;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(l10n.cancelAppointmentTitle),
        content: Text(l10n.cancelAppointmentQuestion),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: Text(l10n.keepAppointment),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(l10n.cancelAppointmentConfirm),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await ref.read(appointmentRepositoryProvider).cancel(item.id);
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(localizedErrorMessage(context, e))));
      }
    } finally {
      ref.invalidate(patientAppointmentsProvider);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final listState = ref.watch(patientAppointmentsProvider);
    final controller = ref.read(patientAppointmentsProvider.notifier);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabAppointments),
        actions: sessionAppBarActions(context, ref),
      ),
      body: Column(
        children: [
          StatusFilterChips(
            selected: controller.filter,
            onChanged: controller.setFilter,
          ),
          Expanded(
            child: listState.when(
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (error, _) => ErrorView(
                error: error,
                onRetry: () => ref.invalidate(patientAppointmentsProvider),
              ),
              data: (data) {
                if (data.items.isEmpty) {
                  return Center(child: Text(l10n.noAppointments));
                }
                return RefreshIndicator(
                  onRefresh: () async =>
                      ref.invalidate(patientAppointmentsProvider),
                  child: ListView.builder(
                    itemCount: data.items.length + (data.isLast ? 0 : 1),
                    itemBuilder: (context, index) {
                      if (index >= data.items.length) {
                        // Tail sentinel: fetch the next page when scrolled
                        // into view (microtask: no state changes in build).
                        Future.microtask(controller.loadMore);
                        return const Padding(
                          padding: EdgeInsets.all(16),
                          child:
                              Center(child: CircularProgressIndicator()),
                        );
                      }
                      final item = data.items[index];
                      final cancellable =
                          item.status == AppointmentStatus.pending ||
                              item.status == AppointmentStatus.accepted;
                      return Card(
                        margin: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 6),
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(item.doctorName,
                                        style: Theme.of(context)
                                            .textTheme
                                            .titleMedium),
                                  ),
                                  AppointmentStatusChip(status: item.status),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(formatAppointmentRange(
                                  context, item.start, item.end)),
                              if (item.reason != null &&
                                  item.reason!.isNotEmpty)
                                Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Text(item.reason!,
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodySmall),
                                ),
                              if (cancellable)
                                Align(
                                  alignment: AlignmentDirectional.centerEnd,
                                  child: TextButton(
                                    onPressed: () =>
                                        _cancel(context, ref, item),
                                    child: Text(
                                        l10n.cancelAppointmentAction),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
