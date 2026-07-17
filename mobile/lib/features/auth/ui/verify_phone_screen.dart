import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../data/auth_repository.dart';
import 'auth_form_widgets.dart';

/// Adds and verifies a phone number for the signed-in user: enter the number and
/// request a code, then confirm it.
class VerifyPhoneScreen extends ConsumerStatefulWidget {
  const VerifyPhoneScreen({super.key});

  @override
  ConsumerState<VerifyPhoneScreen> createState() => _VerifyPhoneScreenState();
}

class _VerifyPhoneScreenState extends ConsumerState<VerifyPhoneScreen> {
  final _formKey = GlobalKey<FormState>();
  final _phone = TextEditingController();
  final _code = TextEditingController();
  bool _codeSent = false;
  String? _error;
  bool _busy = false;

  @override
  void dispose() {
    _phone.dispose();
    _code.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await action();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = localizedErrorMessage(context, e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _sendCode() async {
    final phone = _phone.text.trim();
    if (phone.isEmpty) {
      setState(() => _error = context.l10n.validationRequired);
      return;
    }
    await _run(
      () => ref.read(authRepositoryProvider).requestPhoneVerification(phone),
    );
    if (mounted && _error == null) {
      setState(() => _codeSent = true);
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(context.l10n.codeSent)));
    }
  }

  Future<void> _confirm() async {
    if (!_formKey.currentState!.validate()) return;
    await _run(
      () => ref.read(authRepositoryProvider).confirmPhone(
            phone: _phone.text.trim(),
            code: _code.text.trim(),
          ),
    );
    if (mounted && _error == null) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(context.l10n.phoneVerified)));
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.verifyPhoneTitle)),
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
                  Text(l10n.verifyPhonePrompt),
                  const SizedBox(height: 16),
                  AuthErrorBanner(message: _error),
                  TextFormField(
                    controller: _phone,
                    enabled: !_codeSent,
                    decoration: InputDecoration(labelText: l10n.phoneNumber),
                    keyboardType: TextInputType.phone,
                  ),
                  const SizedBox(height: 12),
                  FilledButton.tonal(
                    onPressed: _busy ? null : _sendCode,
                    child: Text(_codeSent ? l10n.resendCode : l10n.sendCode),
                  ),
                  if (_codeSent) ...[
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _code,
                      decoration:
                          InputDecoration(labelText: l10n.verificationCode),
                      keyboardType: TextInputType.number,
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                              ? l10n.validationRequired
                              : null,
                    ),
                    const SizedBox(height: 16),
                    FilledButton(
                      onPressed: _busy ? null : _confirm,
                      child: _busy
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2))
                          : Text(l10n.verify),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
