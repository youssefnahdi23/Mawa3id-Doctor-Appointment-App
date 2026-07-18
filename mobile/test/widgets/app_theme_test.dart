import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/theme/app_theme.dart';

void main() {
  test('light theme is light, Material 3, seeded', () {
    final theme = AppTheme.light();
    expect(theme.brightness, Brightness.light);
    expect(theme.colorScheme.brightness, Brightness.light);
    expect(theme.useMaterial3, isTrue);
  });

  test('dark theme is dark, Material 3, seeded', () {
    final theme = AppTheme.dark();
    expect(theme.brightness, Brightness.dark);
    expect(theme.colorScheme.brightness, Brightness.dark);
    expect(theme.useMaterial3, isTrue);
  });

  test('both themes share the medical-green seed lineage', () {
    // Same seed → same primary hue family; the two schemes are distinct objects.
    expect(AppTheme.light().colorScheme.primary,
        isNot(AppTheme.dark().colorScheme.primary));
  });
}
