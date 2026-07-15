import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/error_view.dart';
import '../../appointments/state/appointments_providers.dart';
import '../data/record_models.dart';
import '../data/record_repository.dart';
import '../state/records_providers.dart';

/// Doctor form to create or edit the medical record for an appointment.
/// Submitting a new record auto-completes an ACCEPTED appointment on the
/// backend.
class RecordFormScreen extends ConsumerWidget {
  const RecordFormScreen({super.key, required this.appointmentId});

  final int appointmentId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final existing = ref.watch(appointmentRecordProvider(appointmentId));
    return Scaffold(
      appBar: AppBar(title: Text(l10n.recordTitle)),
      body: existing.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () =>
              ref.invalidate(appointmentRecordProvider(appointmentId)),
        ),
        data: (record) =>
            _RecordForm(appointmentId: appointmentId, existing: record),
      ),
    );
  }
}

class _RecordForm extends ConsumerStatefulWidget {
  const _RecordForm({required this.appointmentId, required this.existing});

  final int appointmentId;
  final MedicalRecordResponse? existing;

  @override
  ConsumerState<_RecordForm> createState() => _RecordFormState();
}

class _RecordFormState extends ConsumerState<_RecordForm> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _diagnosis =
      TextEditingController(text: widget.existing?.diagnosis);
  late final TextEditingController _notes =
      TextEditingController(text: widget.existing?.notes);
  late final TextEditingController _prescription =
      TextEditingController(text: widget.existing?.prescription);
  late DateTime? _followUp = widget.existing?.followUpDate;
  bool _submitting = false;

  bool get _isEditing => widget.existing != null;

  @override
  void dispose() {
    _diagnosis.dispose();
    _notes.dispose();
    _prescription.dispose();
    super.dispose();
  }

  Future<void> _pickFollowUp() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _followUp ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 2),
    );
    if (picked != null) setState(() => _followUp = picked);
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    final repo = ref.read(recordRepositoryProvider);
    final body = RecordRequest(
      diagnosis: _diagnosis.text.trim(),
      notes: _notes.text.trim(),
      prescription: _prescription.text.trim(),
      followUpDate: _followUp,
    );
    try {
      if (_isEditing) {
        await repo.update(widget.appointmentId, body);
      } else {
        await repo.create(widget.appointmentId, body);
      }
      // Refresh the record and the doctor queue (a new record completes the
      // appointment).
      ref.invalidate(appointmentRecordProvider(widget.appointmentId));
      ref.invalidate(doctorAppointmentsProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(context.l10n.recordSaved)));
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        setState(() => _submitting = false);
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(localizedErrorMessage(context, e))));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final locale = Localizations.localeOf(context).toString();
    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextFormField(
            controller: _diagnosis,
            decoration: InputDecoration(labelText: l10n.recordDiagnosis),
            maxLength: 1000,
            maxLines: 2,
            textInputAction: TextInputAction.next,
            validator: (value) => (value == null || value.trim().isEmpty)
                ? l10n.recordDiagnosisRequired
                : null,
          ),
          const SizedBox(height: 8),
          TextFormField(
            controller: _notes,
            decoration: InputDecoration(labelText: l10n.recordNotesOptional),
            maxLength: 4000,
            maxLines: 4,
          ),
          const SizedBox(height: 8),
          TextFormField(
            controller: _prescription,
            decoration:
                InputDecoration(labelText: l10n.recordPrescriptionOptional),
            maxLength: 2000,
            maxLines: 3,
          ),
          const SizedBox(height: 8),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.event_outlined),
            title: Text(l10n.recordFollowUpOptional),
            subtitle: Text(_followUp == null
                ? l10n.recordNoFollowUp
                : DateFormat.yMMMMd(locale).format(_followUp!)),
            trailing: _followUp == null
                ? null
                : IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () => setState(() => _followUp = null),
                  ),
            onTap: _pickFollowUp,
          ),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: _submitting
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2))
                : Text(_isEditing ? l10n.save : l10n.recordCompleteVisit),
          ),
        ],
      ),
    );
  }
}
