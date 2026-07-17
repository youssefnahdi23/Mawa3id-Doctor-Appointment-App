import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../data/auth_repository.dart';
import 'auth_form_widgets.dart';

/// Starts a password reset: the user enters any identifier and, if it maps to an
/// account with an email, the backend sends a reset code. The response is uniform
/// regardless, so this always advances to the reset screen.
class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() =>
      _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();
  String? _error;
  bool _submitting = false;

  @override
  void dispose() {
    _identifier.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await ref
          .read(authRepositoryProvider)
          .forgotPassword(_identifier.text.trim());
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(context.l10n.forgotPasswordSent)),
      );
      context.go('/reset-password');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = localizedErrorMessage(context, e));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.forgotPasswordTitle)),
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
                  Text(l10n.forgotPasswordPrompt),
                  const SizedBox(height: 16),
                  AuthErrorBanner(message: _error),
                  TextFormField(
                    controller: _identifier,
                    decoration: InputDecoration(
                      labelText: l10n.identifier,
                      hintText: l10n.identifierHint,
                    ),
                    keyboardType: TextInputType.emailAddress,
                    validator: (value) => value == null || value.trim().isEmpty
                        ? l10n.validationRequired
                        : null,
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: _submitting
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : Text(l10n.sendCode),
                  ),
                  TextButton(
                    onPressed: () => context.go('/reset-password'),
                    child: Text(l10n.haveResetCode),
                  ),
                  TextButton(
                    onPressed: () => context.go('/login'),
                    child: Text(l10n.backToLogin),
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
