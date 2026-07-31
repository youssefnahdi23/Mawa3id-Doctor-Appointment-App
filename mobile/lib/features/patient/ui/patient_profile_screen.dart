import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/patient_models.dart';
import '../data/patient_repository.dart';
import '../state/patient_providers.dart';

/// Lets the signed-in patient view and edit their own profile via
/// `GET`/`PUT /api/patients/me`. Email and patient code are read-only.
class PatientProfileScreen extends ConsumerStatefulWidget {
  const PatientProfileScreen({super.key});

  @override
  ConsumerState<PatientProfileScreen> createState() =>
      _PatientProfileScreenState();
}

class _PatientProfileScreenState extends ConsumerState<PatientProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _fullName = TextEditingController();
  final _phone = TextEditingController();
  DateTime? _dateOfBirth;

  bool _seeded = false;
  bool _saving = false;
  String? _error;
  Map<String, String> _fieldErrors = const {};

  @override
  void dispose() {
    _fullName.dispose();
    _phone.dispose();
    super.dispose();
  }

  void _seed(PatientResponse patient) {
    _fullName.text = patient.fullName;
    _phone.text = patient.phone ?? '';
    _dateOfBirth = patient.dateOfBirth;
    _seeded = true;
  }

  Future<void> _pickDateOfBirth() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _dateOfBirth ?? DateTime(now.year - 30),
      firstDate: DateTime(now.year - 120),
      lastDate: now.subtract(const Duration(days: 1)),
    );
    if (picked != null) setState(() => _dateOfBirth = picked);
  }

  Future<void> _save() async {
    setState(() => _fieldErrors = const {});
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await ref.read(patientRepositoryProvider).updateMe(
            fullName: _fullName.text.trim(),
            dateOfBirth: _dateOfBirth,
            phone: _phone.text.trim(),
          );
      if (!mounted) return;
      ref.invalidate(myPatientProfileProvider);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(context.l10n.profileSaved)));
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _fieldErrors = e.fieldErrors;
        _error =
            e.fieldErrors.isEmpty ? localizedErrorMessage(context, e) : null;
      });
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final profile = ref.watch(myPatientProfileProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabProfile),
        actions: sessionAppBarActions(context, ref),
      ),
      body: profile.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(myPatientProfileProvider),
        ),
        data: (patient) {
          if (!_seeded) _seed(patient);
          return SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _InfoRow(label: l10n.email, value: patient.email),
                      const SizedBox(height: 8),
                      _InfoRow(
                          label: l10n.patientCodeLabel,
                          value: patient.patientCode),
                      const SizedBox(height: 20),
                      if (_error != null)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: Text(
                            _error!,
                            style: TextStyle(
                                color: Theme.of(context).colorScheme.error),
                          ),
                        ),
                      TextFormField(
                        controller: _fullName,
                        decoration: InputDecoration(labelText: l10n.fullName),
                        forceErrorText: _fieldErrors['fullName'],
                        validator: (v) => v == null || v.trim().isEmpty
                            ? l10n.validationRequired
                            : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _phone,
                        decoration:
                            InputDecoration(labelText: l10n.phoneOptional),
                        keyboardType: TextInputType.phone,
                        forceErrorText: _fieldErrors['phone'],
                      ),
                      const SizedBox(height: 16),
                      OutlinedButton.icon(
                        onPressed: _pickDateOfBirth,
                        icon: const Icon(Icons.cake_outlined),
                        label: Text(
                          _dateOfBirth == null
                              ? l10n.dateOfBirth
                              : DateFormat.yMMMd(
                                      Localizations.localeOf(context).toString())
                                  .format(_dateOfBirth!),
                        ),
                      ),
                      if (_fieldErrors['dateOfBirth'] != null)
                        Padding(
                          padding: const EdgeInsetsDirectional.only(
                              start: 12, top: 4),
                          child: Text(
                            _fieldErrors['dateOfBirth']!,
                            style: TextStyle(
                                color: Theme.of(context).colorScheme.error,
                                fontSize: 12),
                          ),
                        ),
                      const SizedBox(height: 24),
                      FilledButton(
                        onPressed: _saving ? null : _save,
                        child: _saving
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child:
                                    CircularProgressIndicator(strokeWidth: 2))
                            : Text(l10n.save),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

/// Read-only label/value pair for identity fields the patient can't change.
class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.outline)),
        Text(value, style: theme.textTheme.bodyMedium),
      ],
    );
  }
}
