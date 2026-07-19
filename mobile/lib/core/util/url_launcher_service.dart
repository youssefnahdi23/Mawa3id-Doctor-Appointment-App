import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

/// Opens a URL in an external browser/app. Wrapped in a provider so screens can
/// stay unit-testable — tests override it with a fake that records the URL
/// instead of hitting the platform channel.
typedef UrlOpener = Future<bool> Function(Uri url);

final urlLauncherProvider = Provider<UrlOpener>(
  (ref) => (url) => launchUrl(url, mode: LaunchMode.externalApplication),
);
