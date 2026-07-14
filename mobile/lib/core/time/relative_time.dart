import 'package:flutter/widgets.dart';
import 'package:intl/intl.dart';

import '../l10n/l10n.dart';

/// "just now" / "5 min ago" / "3 h ago" / "2 d ago", falling back to a
/// localized date past a week. [instant] is a UTC timestamp.
String formatRelativeTime(BuildContext context, DateTime instant,
    {DateTime? now}) {
  final l10n = context.l10n;
  final reference = now ?? DateTime.now();
  final difference = reference.difference(instant.toLocal());
  if (difference.inMinutes < 1) return l10n.relativeJustNow;
  if (difference.inHours < 1) return l10n.relativeMinutes(difference.inMinutes);
  if (difference.inDays < 1) return l10n.relativeHours(difference.inHours);
  if (difference.inDays < 7) return l10n.relativeDays(difference.inDays);
  return DateFormat.yMMMd(Localizations.localeOf(context).toString())
      .format(instant.toLocal());
}
