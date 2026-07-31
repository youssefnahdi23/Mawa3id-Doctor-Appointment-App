import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/error_view.dart';
import '../data/appointment_models.dart';
import '../data/appointment_repository.dart';
import '../state/appointments_providers.dart';

class SlotPickerScreen extends ConsumerStatefulWidget {
  const SlotPickerScreen({
    super.key,
    required this.doctorId,
    this.rescheduleAppointmentId,
  });

  final int doctorId;

  /// When set, picking a slot moves this appointment instead of booking a new one.
  final int? rescheduleAppointmentId;

  @override
  ConsumerState<SlotPickerScreen> createState() => _SlotPickerScreenState();
}

class _SlotPickerScreenState extends ConsumerState<SlotPickerScreen> {
  DateTime? _selectedDay;

  Future<void> _pickSlot(SlotResponse slot) async {
    final result = await showModalBottomSheet<AppointmentResponse>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _BookingConfirmSheet(
        doctorId: widget.doctorId,
        slot: slot,
        rescheduleAppointmentId: widget.rescheduleAppointmentId,
      ),
    );
    if (result == null || !mounted) return;
    // A conflict (409) already refreshed the slots inside the sheet; success
    // lands here.
    final l10n = context.l10n;
    if (widget.rescheduleAppointmentId != null) {
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.appointmentRescheduled)));
      context.pop();
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(result.status == AppointmentStatus.accepted
          ? l10n.bookingConfirmed
          : l10n.bookingPending),
    ));
    context.go('/p/appointments');
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final slots = ref.watch(slotsProvider(widget.doctorId));
    final locale = Localizations.localeOf(context).toString();
    return Scaffold(
      appBar: AppBar(title: Text(l10n.chooseSlotTitle)),
      body: slots.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(slotsProvider(widget.doctorId)),
        ),
        data: (byDay) {
          if (byDay.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(l10n.noSlotsAvailable,
                    textAlign: TextAlign.center),
              ),
            );
          }
          final days = byDay.keys.toList()..sort();
          final selectedDay =
              _selectedDay != null && byDay.containsKey(_selectedDay)
                  ? _selectedDay!
                  : days.first;
          final daySlots = byDay[selectedDay]!;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(AppSpacing.lg,
                    AppSpacing.lg, AppSpacing.lg, AppSpacing.sm),
                child: Text(l10n.selectDate,
                    style: Theme.of(context)
                        .textTheme
                        .titleMedium
                        ?.copyWith(fontWeight: FontWeight.w700)),
              ),
              SizedBox(
                height: 76,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.lg),
                  itemCount: days.length,
                  separatorBuilder: (_, __) =>
                      const SizedBox(width: AppSpacing.sm),
                  itemBuilder: (context, i) => _DayCard(
                    day: days[i],
                    locale: locale,
                    selected: days[i] == selectedDay,
                    onTap: () => setState(() => _selectedDay = days[i]),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(AppSpacing.lg,
                    AppSpacing.lg, AppSpacing.lg, AppSpacing.sm),
                child: Text(l10n.availableSlots,
                    style: Theme.of(context)
                        .textTheme
                        .titleMedium
                        ?.copyWith(fontWeight: FontWeight.w700)),
              ),
              Expanded(
                child: GridView.count(
                  padding: const EdgeInsets.fromLTRB(AppSpacing.lg, 0,
                      AppSpacing.lg, AppSpacing.xl),
                  crossAxisCount: 3,
                  mainAxisSpacing: AppSpacing.md,
                  crossAxisSpacing: AppSpacing.md,
                  childAspectRatio: 2.4,
                  children: [
                    for (final slot in daySlots)
                      _SlotTile(
                        label: DateFormat.Hm(locale).format(slot.start),
                        onTap: () => _pickSlot(slot),
                      ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

/// A tappable date card (weekday over day-of-month) in the slot picker's date
/// rail; fills green when selected.
class _DayCard extends StatelessWidget {
  const _DayCard({
    required this.day,
    required this.locale,
    required this.selected,
    required this.onTap,
  });

  final DateTime day;
  final String locale;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      borderRadius: AppRadii.cardR,
      child: Container(
        width: 60,
        decoration: BoxDecoration(
          color: selected ? colors.primary : colors.surfaceContainerLow,
          borderRadius: AppRadii.cardR,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              DateFormat.E(locale).format(day).toUpperCase(),
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: selected
                        ? colors.onPrimary.withValues(alpha: 0.9)
                        : colors.onSurfaceVariant,
                  ),
            ),
            const SizedBox(height: 2),
            Text(
              DateFormat.d(locale).format(day),
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: selected ? colors.onPrimary : colors.onSurface,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

/// A time-slot tile in the availability grid.
class _SlotTile extends StatelessWidget {
  const _SlotTile({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Material(
      color: colors.surfaceContainerHigh,
      borderRadius: AppRadii.chipR,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Center(
          child: Text(
            label,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(fontWeight: FontWeight.w600),
          ),
        ),
      ),
    );
  }
}

class _BookingConfirmSheet extends ConsumerStatefulWidget {
  const _BookingConfirmSheet({
    required this.doctorId,
    required this.slot,
    this.rescheduleAppointmentId,
  });

  final int doctorId;
  final SlotResponse slot;
  final int? rescheduleAppointmentId;

  bool get isReschedule => rescheduleAppointmentId != null;

  @override
  ConsumerState<_BookingConfirmSheet> createState() =>
      _BookingConfirmSheetState();
}

class _BookingConfirmSheetState extends ConsumerState<_BookingConfirmSheet> {
  final _reason = TextEditingController();
  String? _error;
  bool _submitting = false;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _confirm() async {
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final repo = ref.read(appointmentRepositoryProvider);
      final result = widget.isReschedule
          ? await repo.reschedule(
              widget.rescheduleAppointmentId!, widget.slot.start)
          : await repo.book(
              doctorId: widget.doctorId,
              startTime: widget.slot.start,
              reason: _reason.text.trim(),
            );
      if (mounted) Navigator.of(context).pop(result);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.kind == ApiErrorKind.conflict) {
        // Someone else took the slot: refresh the picker behind the sheet.
        ref.invalidate(slotsProvider(widget.doctorId));
        setState(() => _error = context.l10n.errorSlotTaken);
      } else {
        setState(() => _error = localizedErrorMessage(context, e));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final locale = Localizations.localeOf(context).toString();
    final slot = widget.slot;
    final when = '${DateFormat.yMMMEd(locale).format(slot.start)} · '
        '${DateFormat.Hm(locale).format(slot.start)}–'
        '${DateFormat.Hm(locale).format(slot.end)}';
    return Padding(
      padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
                widget.isReschedule
                    ? l10n.rescheduleConfirmTitle
                    : l10n.confirmBookingTitle,
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(when, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 16),
            if (!widget.isReschedule)
              TextField(
                controller: _reason,
                decoration:
                    InputDecoration(labelText: l10n.reasonOptional),
                maxLength: 500,
                maxLines: 2,
              ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(
                  _error!,
                  style:
                      TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ),
            FilledButton(
              onPressed: _submitting ? null : _confirm,
              child: _submitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2))
                  : Text(widget.isReschedule
                      ? l10n.rescheduleConfirm
                      : l10n.confirmBooking),
            ),
          ],
        ),
      ),
    );
  }
}
