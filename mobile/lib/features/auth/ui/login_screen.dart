import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/widgets/settings_sheet.dart';
import '../data/auth_repository.dart';
import '../state/session_controller.dart';
import 'auth_form_widgets.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifier = TextEditingController();
  final _password = TextEditingController();
  String? _error;
  bool _submitting = false;

  @override
  void dispose() {
    _identifier.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final auth = await ref.read(authRepositoryProvider).login(
            identifier: _identifier.text.trim(),
            password: _password.text,
          );
      await ref.read(sessionControllerProvider.notifier).signIn(auth);
      // Router redirect takes over from here.
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.kind == ApiErrorKind.unauthorized
            ? context.l10n.errorBadCredentials
            : localizedErrorMessage(context, e);
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
        title: Text(l10n.appTitle),
        actions: const [SettingsButton()],
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
                  Center(
                    child: SvgPicture.asset(
                      'assets/images/mawa3id_logo.svg',
                      height: 96,
                      width: 96,
                    ),
                  ),
                  const SizedBox(height: 24),
                  Text(l10n.loginTitle,
                      style: Theme.of(context).textTheme.headlineSmall,
                      textAlign: TextAlign.center),
                  const SizedBox(height: 24),
                  AuthErrorBanner(message: _error),
                  TextFormField(
                    controller: _identifier,
                    decoration: InputDecoration(
                      labelText: l10n.identifier,
                      hintText: l10n.identifierHint,
                    ),
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.username],
                    validator: (value) => value == null || value.trim().isEmpty
                        ? l10n.validationRequired
                        : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _password,
                    decoration: InputDecoration(labelText: l10n.password),
                    obscureText: true,
                    autofillHints: const [AutofillHints.password],
                    onFieldSubmitted: (_) => _submit(),
                    validator: (value) => value == null || value.isEmpty
                        ? l10n.validationRequired
                        : null,
                  ),
                  Align(
                    alignment: AlignmentDirectional.centerEnd,
                    child: TextButton(
                      onPressed: () => context.go('/forgot-password'),
                      child: Text(l10n.forgotPassword),
                    ),
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: _submitting
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : Text(l10n.signIn),
                  ),
                  const SizedBox(height: 20),
                  _OrDivider(label: l10n.orDivider),
                  const SizedBox(height: 20),
                  // Social login is deferred: the backend has no OAuth endpoint
                  // yet (see auth-system scope), so this button is inert for now.
                  OutlinedButton.icon(
                    onPressed: () {
                      ScaffoldMessenger.of(context)
                        ..hideCurrentSnackBar()
                        ..showSnackBar(
                          SnackBar(content: Text(l10n.featureComingSoon)),
                        );
                    },
                    icon: SvgPicture.asset(
                      'assets/images/google_g.svg',
                      height: 20,
                      width: 20,
                    ),
                    label: Text(l10n.continueWithGoogle),
                  ),
                  const SizedBox(height: 24),
                  TextButton(
                    onPressed: () => context.go('/register/patient'),
                    child: Text(l10n.registerAsPatient),
                  ),
                  TextButton(
                    onPressed: () => context.go('/register/doctor'),
                    child: Text(l10n.registerAsDoctor),
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

/// A horizontal rule with a centered label, e.g. "— or —".
class _OrDivider extends StatelessWidget {
  const _OrDivider({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Divider()),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            label,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
        const Expanded(child: Divider()),
      ],
    );
  }
}
