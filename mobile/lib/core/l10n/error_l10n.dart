import 'package:flutter/widgets.dart';

import '../network/api_exception.dart';
import 'l10n.dart';

/// Localized user-facing text for a caught error. Prefers a message keyed
/// off the error kind; falls back to the server's (English) message for
/// bad requests, then to the generic message.
String localizedErrorMessage(BuildContext context, Object error) {
  final l10n = context.l10n;
  if (error is ApiException) {
    return switch (error.kind) {
      ApiErrorKind.network => l10n.errorNetwork,
      ApiErrorKind.rateLimited => l10n.errorRateLimited,
      ApiErrorKind.unauthorized => l10n.errorUnauthorized,
      ApiErrorKind.conflict => error.message ?? l10n.errorConflict,
      ApiErrorKind.badRequest => error.message ?? l10n.errorGeneric,
      _ => l10n.errorGeneric,
    };
  }
  return l10n.errorGeneric;
}
