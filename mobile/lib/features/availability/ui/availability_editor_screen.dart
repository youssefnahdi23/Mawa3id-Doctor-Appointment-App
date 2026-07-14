import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/session_actions.dart';
import '../../auth/state/session_controller.dart';
import '../data/availability_models.dart';
import '../data/availability_repository.dart';

final myAvailabilityProvider =
    FutureProvider.autoDispose<List<AvailabilityRule>>((ref) async {
  final session = await ref.watch(sessionControllerProvider.future);
  if (session == null) return const [];
  return ref.watch(availabilityRepositoryProvider).forDoctor(session.userId);
});

/// Minimal weekly-availability editor: one row per rule (day + start/end),
/// saved wholesale with `PUT /api/doctors/me/availability`.
class AvailabilityEditorScreen extends ConsumerStatefulWidget {
  const AvailabilityEditorScreen({super.key});

  @override
  ConsumerState<AvailabilityEditorScreen> createState() =>
      _AvailabilityEditorScreenState();
}

class _AvailabilityEditorScreenState
    extends ConsumerState<AvailabilityEditorScreen> {
  List<AvailabilityRule>? _rules;
  bool _saving = false;

  /// Localized weekday name via a fixed week that starts on a Monday.
  String _dayName(BuildContext context, Weekday day) {
    final locale = Localizations.localeOf(context).toString();
    final monday = DateTime(2024, 1, 1); // a Monday
    return DateFormat.EEEE(locale)
        .format(DateTime(2024, 1, monday.day + day.index));
  }

  String? _validationError(BuildContext context) {
    for (final rule in _rules!) {
      if (rule.startMinutes >= rule.endMinutes) {
        return context.l10n.validationTimeRange;
      }
    }
    return null;
  }

  Future<void> _save() async {
    final error = _validationError(context);
    if (error != null) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(error)));
      return;
    }
    setState(() => _saving = true);
    try {
      final saved = await ref
          .read(availabilityRepositoryProvider)
          .updateMine(_rules!);
      if (!mounted) return;
      setState(() => _rules = saved);
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.availabilitySaved)));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(localizedErrorMessage(context, e))));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _editTime(int index, {required bool isStart}) async {
    final rule = _rules![index];
    final minutes = isStart ? rule.startMinutes : rule.endMinutes;
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(hour: minutes ~/ 60, minute: minutes % 60),
    );
    if (picked == null) return;
    final formatted = formatMinutesAsTime(picked.hour * 60 + picked.minute);
    setState(() {
      _rules![index] = AvailabilityRule(
        dayOfWeek: rule.dayOfWeek,
        startTime: isStart ? formatted : rule.startTime,
        endTime: isStart ? rule.endTime : formatted,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final loaded = ref.watch(myAvailabilityProvider);

    // Seed the editable copy once the initial load lands.
    if (_rules == null && loaded.hasValue) {
      _rules = List.of(loaded.value!);
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabAvailability),
        actions: sessionAppBarActions(context, ref),
      ),
      body: _rules == null
          ? loaded.when(
              loading: () =>
                  const Center(child: CircularProgressIndicator()),
              error: (error, _) => ErrorView(
                error: error,
                onRetry: () => ref.invalidate(myAvailabilityProvider),
              ),
              data: (_) => const SizedBox.shrink(),
            )
          : Column(
              children: [
                Expanded(
                  child: _rules!.isEmpty
                      ? Center(
                          child: Padding(
                            padding: const EdgeInsets.all(24),
                            child: Text(l10n.availabilityEmptyHint,
                                textAlign: TextAlign.center),
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(8),
                          itemCount: _rules!.length,
                          itemBuilder: (context, index) {
                            final rule = _rules![index];
                            return Card(
                              child: Padding(
                                padding: const EdgeInsetsDirectional
                                    .fromSTEB(16, 4, 4, 4),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: DropdownButton<Weekday>(
                                        value: rule.dayOfWeek,
                                        isExpanded: true,
                                        underline: const SizedBox.shrink(),
                                        items: [
                                          for (final day in Weekday.values)
                                            DropdownMenuItem(
                                              value: day,
                                              child: Text(
                                                  _dayName(context, day)),
                                            ),
                                        ],
                                        onChanged: (day) {
                                          if (day == null) return;
                                          setState(() {
                                            _rules![index] =
                                                AvailabilityRule(
                                              dayOfWeek: day,
                                              startTime: rule.startTime,
                                              endTime: rule.endTime,
                                            );
                                          });
                                        },
                                      ),
                                    ),
                                    TextButton(
                                      onPressed: () =>
                                          _editTime(index, isStart: true),
                                      child: Text(formatMinutesAsTime(
                                          rule.startMinutes)),
                                    ),
                                    const Text('–'),
                                    TextButton(
                                      onPressed: () =>
                                          _editTime(index, isStart: false),
                                      child: Text(formatMinutesAsTime(
                                          rule.endMinutes)),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.delete_outline),
                                      tooltip: l10n.removeRule,
                                      onPressed: () => setState(
                                          () => _rules!.removeAt(index)),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                ),
                SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            icon: const Icon(Icons.add),
                            label: Text(l10n.addRule),
                            onPressed: () => setState(() {
                              _rules!.add(const AvailabilityRule(
                                dayOfWeek: Weekday.monday,
                                startTime: '09:00',
                                endTime: '17:00',
                              ));
                            }),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: FilledButton(
                            onPressed: _saving ? null : _save,
                            child: _saving
                                ? const SizedBox(
                                    height: 20,
                                    width: 20,
                                    child: CircularProgressIndicator(
                                        strokeWidth: 2))
                                : Text(l10n.save),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}
