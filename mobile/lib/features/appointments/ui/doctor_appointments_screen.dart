import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/widgets/empty_state.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/loading_skeleton.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/appointment_models.dart';
import '../data/appointment_repository.dart';
import '../state/appointments_providers.dart';
import 'appointment_widgets.dart';

class DoctorAppointmentsScreen extends ConsumerWidget {
  const DoctorAppointmentsScreen({super.key});

  Future<void> _transition(
    BuildContext context,
    WidgetRef ref,
    Future<AppointmentResponse> Function(AppointmentRepository) action,
  ) async {
    try {
      await action(ref.read(appointmentRepositoryProvider));
    } on ApiException catch (e) {
      if (context.mounted) {
        // A 409 means the appointment changed under us (e.g. the patient
        // cancelled first); the refetch below shows the real state.
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(e.kind == ApiErrorKind.conflict
                ? context.l10n.errorAppointmentChanged
                : localizedErrorMessage(context, e))));
      }
    } finally {
      ref.invalidate(doctorAppointmentsProvider);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final listState = ref.watch(doctorAppointmentsProvider);
    final controller = ref.read(doctorAppointmentsProvider.notifier);
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
              loading: () => const SkeletonList(),
              error: (error, _) => ErrorView(
                error: error,
                onRetry: () => ref.invalidate(doctorAppointmentsProvider),
              ),
              data: (data) {
                if (data.items.isEmpty) {
                  return EmptyState(
                    icon: Icons.event_busy,
                    title: l10n.noAppointments,
                  );
                }
                return RefreshIndicator(
                  onRefresh: () async =>
                      ref.invalidate(doctorAppointmentsProvider),
                  child: ListView.builder(
                    itemCount: data.items.length + (data.isLast ? 0 : 1),
                    itemBuilder: (context, index) {
                      if (index >= data.items.length) {
                        Future.microtask(controller.loadMore);
                        return const Padding(
                          padding: EdgeInsets.all(16),
                          child:
                              Center(child: CircularProgressIndicator()),
                        );
                      }
                      final item = data.items[index];
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
                                    child: Text(item.patientName,
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
                              _ActionRow(
                                status: item.status,
                                onAction: (action) =>
                                    _transition(context, ref,
                                        (repo) => action(repo, item.id)),
                                onRecord: () => context
                                    .go('/d/appointments/${item.id}/record'),
                                onReschedule: () => context
                                    .push('/d/appointments/${item.id}'
                                        '/reschedule?doctorId=${item.doctorId}')
                                    .then((_) => ref.invalidate(
                                        doctorAppointmentsProvider)),
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

typedef _RepoAction = Future<AppointmentResponse> Function(
    AppointmentRepository repo, int id);

/// Buttons legal for the appointment's current status. PENDING → accept/reject;
/// ACCEPTED → complete-with-notes (opens the record form, which completes the
/// visit) / cancel; COMPLETED → view/edit the record.
class _ActionRow extends StatelessWidget {
  const _ActionRow({
    required this.status,
    required this.onAction,
    required this.onRecord,
    required this.onReschedule,
  });

  final AppointmentStatus status;
  final void Function(_RepoAction) onAction;
  final VoidCallback onRecord;
  final VoidCallback onReschedule;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final children = <Widget>[];
    switch (status) {
      case AppointmentStatus.pending:
        children.add(FilledButton.tonal(
            onPressed: () => onAction((repo, id) => repo.accept(id)),
            child: Text(l10n.actionAccept)));
        children.add(TextButton(
            onPressed: () => onAction((repo, id) => repo.reject(id)),
            child: Text(l10n.actionReject)));
        children.add(TextButton(
            onPressed: onReschedule, child: Text(l10n.actionReschedule)));
      case AppointmentStatus.accepted:
        children.add(FilledButton.tonal(
            onPressed: onRecord, child: Text(l10n.recordCompleteWithNotes)));
        children.add(TextButton(
            onPressed: () => onAction((repo, id) => repo.noShow(id)),
            child: Text(l10n.actionNoShow)));
        children.add(TextButton(
            onPressed: onReschedule, child: Text(l10n.actionReschedule)));
        children.add(TextButton(
            onPressed: () => onAction((repo, id) => repo.cancel(id)),
            child: Text(l10n.actionCancel)));
      case AppointmentStatus.completed:
        children.add(TextButton(
            onPressed: onRecord, child: Text(l10n.recordViewEdit)));
      case AppointmentStatus.rejected:
      case AppointmentStatus.cancelled:
      case AppointmentStatus.noShow:
        break;
    }
    if (children.isEmpty) return const SizedBox.shrink();
    return Align(
      alignment: AlignmentDirectional.centerEnd,
      child: Wrap(spacing: 8, children: children),
    );
  }
}
