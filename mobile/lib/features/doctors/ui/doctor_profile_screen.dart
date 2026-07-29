import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/doctor_models.dart';
import '../data/doctor_reference.dart';
import '../data/doctor_repository.dart';
import '../state/doctors_providers.dart';

/// Lets the signed-in doctor view and edit their own profile via
/// `GET`/`PUT /api/doctors/me`, including the booking settings (acceptance mode
/// and slot duration) that are otherwise unsettable in the app.
class DoctorProfileScreen extends ConsumerStatefulWidget {
  const DoctorProfileScreen({super.key});

  @override
  ConsumerState<DoctorProfileScreen> createState() =>
      _DoctorProfileScreenState();
}

class _DoctorProfileScreenState extends ConsumerState<DoctorProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _cabinetAddress = TextEditingController();
  final _workingHours = TextEditingController();
  final _phone = TextEditingController();
  final _bio = TextEditingController();
  final _slotDuration = TextEditingController();
  final _consultationFee = TextEditingController();
  int? _specialtyId;
  AcceptanceMode _acceptanceMode = AcceptanceMode.manual;
  bool _cnamConventionne = false;
  String? _governorate;
  final Set<String> _languages = {};

  bool _seeded = false;
  bool _saving = false;
  String? _error;
  Map<String, String> _fieldErrors = const {};

  @override
  void dispose() {
    _name.dispose();
    _cabinetAddress.dispose();
    _workingHours.dispose();
    _phone.dispose();
    _bio.dispose();
    _slotDuration.dispose();
    _consultationFee.dispose();
    super.dispose();
  }

  void _seed(DoctorResponse doctor) {
    _name.text = doctor.name;
    _cabinetAddress.text = doctor.cabinetAddress ?? '';
    _workingHours.text = doctor.workingHours ?? '';
    _phone.text = doctor.phone ?? '';
    _bio.text = doctor.bio ?? '';
    _slotDuration.text = doctor.slotDurationMinutes.toString();
    _consultationFee.text = doctor.consultationFee?.toString() ?? '';
    _specialtyId = doctor.specialty?.id;
    _acceptanceMode = doctor.acceptanceMode;
    _cnamConventionne = doctor.cnamConventionne;
    _governorate = doctor.governorate;
    _languages
      ..clear()
      ..addAll(doctor.languages);
    _seeded = true;
  }

  Future<void> _save() async {
    setState(() => _fieldErrors = const {});
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await ref.read(doctorRepositoryProvider).updateMe(
            name: _name.text.trim(),
            specialtyId: _specialtyId,
            cabinetAddress: _cabinetAddress.text.trim(),
            workingHours: _workingHours.text.trim(),
            phone: _phone.text.trim(),
            bio: _bio.text.trim(),
            acceptanceMode: _acceptanceMode,
            slotDurationMinutes: int.tryParse(_slotDuration.text.trim()),
            cnamConventionne: _cnamConventionne,
            governorate: _governorate,
            consultationFee: int.tryParse(_consultationFee.text.trim()),
            languages: _languages.toList(),
          );
      if (!mounted) return;
      ref.invalidate(myDoctorProfileProvider);
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
    final languageCode = Localizations.localeOf(context).languageCode;
    final profile = ref.watch(myDoctorProfileProvider);
    final specialties = ref.watch(specialtiesProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabProfile),
        actions: sessionAppBarActions(context, ref),
      ),
      body: profile.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(myDoctorProfileProvider),
        ),
        data: (doctor) {
          if (!_seeded) _seed(doctor);
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
                      _ReadOnlyHeader(
                        title: doctor.email,
                        subtitle:
                            '★ ${doctor.ratingAverage.toStringAsFixed(1)}'
                            ' (${doctor.ratingCount})',
                      ),
                      const SizedBox(height: 16),
                      if (_error != null) _ErrorBanner(message: _error!),
                      TextFormField(
                        controller: _name,
                        decoration: InputDecoration(labelText: l10n.doctorName),
                        forceErrorText: _fieldErrors['name'],
                        validator: (v) => v == null || v.trim().isEmpty
                            ? l10n.validationRequired
                            : null,
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<int>(
                        initialValue: _specialtyId,
                        decoration: InputDecoration(
                          labelText: l10n.specialty,
                          errorText: _fieldErrors['specialtyId'],
                        ),
                        items: [
                          for (final s in specialties.valueOrNull ?? [])
                            DropdownMenuItem(
                                value: s.id,
                                child: Text(s.localizedName(languageCode))),
                        ],
                        onChanged: (v) => setState(() => _specialtyId = v),
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String?>(
                        initialValue: _governorate,
                        decoration:
                            InputDecoration(labelText: l10n.governorate),
                        items: [
                          DropdownMenuItem(
                              value: null, child: Text(l10n.noGovernorate)),
                          for (final code in kGovernorates)
                            DropdownMenuItem(
                                value: code,
                                child: Text(governorateLabel(l10n, code))),
                        ],
                        onChanged: (v) => setState(() => _governorate = v),
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _cabinetAddress,
                        decoration: InputDecoration(
                            labelText: l10n.cabinetAddressOptional),
                        forceErrorText: _fieldErrors['cabinetAddress'],
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _workingHours,
                        decoration: InputDecoration(
                            labelText: l10n.workingHoursOptional),
                        forceErrorText: _fieldErrors['workingHours'],
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _phone,
                        decoration:
                            InputDecoration(labelText: l10n.phoneOptional),
                        keyboardType: TextInputType.phone,
                        forceErrorText: _fieldErrors['phone'],
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _bio,
                        decoration: InputDecoration(labelText: l10n.bioOptional),
                        minLines: 2,
                        maxLines: 5,
                        forceErrorText: _fieldErrors['bio'],
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _consultationFee,
                        decoration: InputDecoration(
                          labelText: l10n.consultationFee,
                          helperText: l10n.consultationFeeHint,
                          suffixText: l10n.consultationFeeUnit,
                        ),
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly
                        ],
                        forceErrorText: _fieldErrors['consultationFee'],
                      ),
                      const SizedBox(height: 8),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: Text(l10n.cnamConventionneLabel),
                        value: _cnamConventionne,
                        onChanged: (v) =>
                            setState(() => _cnamConventionne = v),
                      ),
                      const SizedBox(height: 8),
                      Text(l10n.languagesSpoken,
                          style: Theme.of(context).textTheme.labelLarge),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        children: [
                          for (final code in kSpokenLanguages)
                            FilterChip(
                              label: Text(languageLabel(l10n, code)),
                              selected: _languages.contains(code),
                              onSelected: (selected) => setState(() {
                                if (selected) {
                                  _languages.add(code);
                                } else {
                                  _languages.remove(code);
                                }
                              }),
                            ),
                        ],
                      ),
                      const SizedBox(height: 20),
                      Text(l10n.acceptanceModeLabel,
                          style: Theme.of(context).textTheme.labelLarge),
                      const SizedBox(height: 8),
                      SegmentedButton<AcceptanceMode>(
                        segments: [
                          ButtonSegment(
                            value: AcceptanceMode.manual,
                            label: Text(l10n.acceptanceModeManual),
                          ),
                          ButtonSegment(
                            value: AcceptanceMode.auto,
                            label: Text(l10n.acceptanceModeAuto),
                          ),
                        ],
                        selected: {_acceptanceMode},
                        onSelectionChanged: (s) =>
                            setState(() => _acceptanceMode = s.first),
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _slotDuration,
                        decoration: InputDecoration(
                          labelText: l10n.slotDurationLabel,
                          helperText: l10n.slotDurationHint,
                        ),
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly
                        ],
                        forceErrorText: _fieldErrors['slotDurationMinutes'],
                        validator: (v) {
                          final n = int.tryParse((v ?? '').trim());
                          if (n == null || n < 5 || n > 240) {
                            return l10n.slotDurationHint;
                          }
                          return null;
                        },
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

/// Inline banner for whole-request save failures (network, rate limit, etc.).
class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      margin: const EdgeInsetsDirectional.only(bottom: 16),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(message, style: TextStyle(color: colors.onErrorContainer)),
    );
  }
}

/// Read-only identity strip shown above the editable fields.
class _ReadOnlyHeader extends StatelessWidget {
  const _ReadOnlyHeader({required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: theme.textTheme.titleMedium),
        const SizedBox(height: 2),
        Text(subtitle,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.outline)),
      ],
    );
  }
}
