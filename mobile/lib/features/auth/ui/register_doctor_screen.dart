import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../doctors/state/doctors_providers.dart';
import '../data/auth_repository.dart';
import '../state/session_controller.dart';
import 'auth_form_widgets.dart';

class RegisterDoctorScreen extends ConsumerStatefulWidget {
  const RegisterDoctorScreen({super.key});

  @override
  ConsumerState<RegisterDoctorScreen> createState() =>
      _RegisterDoctorScreenState();
}

class _RegisterDoctorScreenState extends ConsumerState<RegisterDoctorScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _cabinetAddress = TextEditingController();
  final _phone = TextEditingController();
  int? _specialtyId;
  String? _error;
  Map<String, String> _fieldErrors = const {};
  bool _submitting = false;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    _cabinetAddress.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _fieldErrors = const {});
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final auth = await ref.read(authRepositoryProvider).registerDoctor(
            email: _email.text.trim(),
            password: _password.text,
            name: _name.text.trim(),
            specialtyId: _specialtyId!,
            cabinetAddress: _cabinetAddress.text.trim(),
            phone: _phone.text.trim(),
          );
      await ref.read(sessionControllerProvider.notifier).signIn(auth);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _fieldErrors = e.fieldErrors;
        _error = e.fieldErrors.isEmpty
            ? localizedErrorMessage(context, e)
            : null;
      });
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final specialties = ref.watch(specialtiesProvider);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.registerAsDoctor),
        leading: BackButton(onPressed: () => context.go('/login')),
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  AuthErrorBanner(message: _error),
                  TextFormField(
                    controller: _name,
                    decoration: InputDecoration(labelText: l10n.doctorName),
                    forceErrorText: _fieldErrors['name'],
                    validator: (value) =>
                        value == null || value.trim().isEmpty
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
                      for (final specialty in specialties.valueOrNull ?? [])
                        DropdownMenuItem(
                          value: specialty.id,
                          child: Text(specialty.name),
                        ),
                    ],
                    onChanged: (value) => setState(() => _specialtyId = value),
                    validator: (value) =>
                        value == null ? l10n.validationSpecialty : null,
                  ),
                  if (specialties.hasError)
                    Padding(
                      padding: const EdgeInsetsDirectional.only(top: 4),
                      child: Text(
                        localizedErrorMessage(context, specialties.error!),
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.error,
                            fontSize: 12),
                      ),
                    ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _email,
                    decoration: InputDecoration(labelText: l10n.email),
                    keyboardType: TextInputType.emailAddress,
                    forceErrorText: _fieldErrors['email'],
                    validator: (value) =>
                        value == null || !value.contains('@')
                            ? l10n.validationEmail
                            : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _password,
                    decoration: InputDecoration(
                      labelText: l10n.password,
                      helperText: l10n.passwordHint,
                    ),
                    obscureText: true,
                    forceErrorText: _fieldErrors['password'],
                    validator: (value) => value == null || value.length < 8
                        ? l10n.validationPassword
                        : null,
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
                    controller: _phone,
                    decoration:
                        InputDecoration(labelText: l10n.phoneOptional),
                    keyboardType: TextInputType.phone,
                    forceErrorText: _fieldErrors['phone'],
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: _submitting
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : Text(l10n.createAccount),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
