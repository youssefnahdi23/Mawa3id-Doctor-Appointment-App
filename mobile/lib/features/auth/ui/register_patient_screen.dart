import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../data/auth_repository.dart';
import '../state/session_controller.dart';
import 'auth_form_widgets.dart';

class RegisterPatientScreen extends ConsumerStatefulWidget {
  const RegisterPatientScreen({super.key});

  @override
  ConsumerState<RegisterPatientScreen> createState() =>
      _RegisterPatientScreenState();
}

class _RegisterPatientScreenState
    extends ConsumerState<RegisterPatientScreen> {
  final _formKey = GlobalKey<FormState>();
  final _fullName = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  DateTime? _dateOfBirth;
  String? _error;
  Map<String, String> _fieldErrors = const {};
  bool _submitting = false;

  @override
  void dispose() {
    _fullName.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
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

  Future<void> _submit() async {
    setState(() => _fieldErrors = const {});
    if (!_formKey.currentState!.validate()) return;
    if (_dateOfBirth == null) {
      setState(() => _error = context.l10n.validationDateOfBirth);
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final auth = await ref.read(authRepositoryProvider).registerPatient(
            email: _email.text.trim(),
            password: _password.text,
            fullName: _fullName.text.trim(),
            dateOfBirth: _dateOfBirth!,
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
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.registerAsPatient),
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
                    controller: _fullName,
                    decoration: InputDecoration(labelText: l10n.fullName),
                    forceErrorText: _fieldErrors['fullName'],
                    validator: (value) =>
                        value == null || value.trim().isEmpty
                            ? l10n.validationRequired
                            : null,
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
