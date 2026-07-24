// Driver for the store-screenshot harness (integration_test/screenshots_test.dart).
// Writes each captured PNG to build/screenshots/<name>.png, where <name> is
// "<locale>/<screen>" set by the harness. CI maps these into the fastlane
// screenshot folders per platform.
import 'dart:io';

import 'package:integration_test/integration_test_driver_extended.dart';

Future<void> main() async {
  await integrationDriver(
    onScreenshot: (String name, List<int> bytes, [Map<String, Object?>? args]) async {
      final file = File('build/screenshots/$name.png');
      await file.create(recursive: true);
      await file.writeAsBytes(bytes);
      return true;
    },
  );
}
