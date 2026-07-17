import 'package:flutter/material.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';

/// Merge the backend's field errors with a locally-derived username conflict.
///
/// A taken username comes back as a 409 with no `fieldErrors` (same shape as a
/// duplicate email), so we route that specific conflict onto the `username`
/// field for inline display; every other error is left untouched for the caller
/// to show in the banner.
Map<String, String> usernameConflictErrors(
    ApiException e, AppLocalizations l10n) {
  final errors = <String, String>{...e.fieldErrors};
  if (e.kind == ApiErrorKind.conflict &&
      e.fieldErrors.isEmpty &&
      (e.message ?? '').toLowerCase().contains('username')) {
    errors['username'] = l10n.usernameTaken;
  }
  return errors;
}

/// Red banner above auth forms for whole-request failures (bad credentials,
/// rate limit, network); field-level errors go on the fields instead.
class AuthErrorBanner extends StatelessWidget {
  const AuthErrorBanner({super.key, required this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    if (message == null) return const SizedBox.shrink();
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      margin: const EdgeInsetsDirectional.only(bottom: 16),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(message!, style: TextStyle(color: colors.onErrorContainer)),
    );
  }
}
