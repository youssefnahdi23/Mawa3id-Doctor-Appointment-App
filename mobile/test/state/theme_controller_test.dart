import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/theme/theme_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  test('defaults to system when nothing is stored', () async {
    SharedPreferences.setMockInitialValues({});
    final container = ProviderContainer();
    addTearDown(container.dispose);

    expect(container.read(themeControllerProvider), ThemeMode.system);
  });

  test('set persists the chosen mode and updates state', () async {
    SharedPreferences.setMockInitialValues({});
    final container = ProviderContainer();
    addTearDown(container.dispose);

    await container.read(themeControllerProvider.notifier).set(ThemeMode.dark);

    expect(container.read(themeControllerProvider), ThemeMode.dark);
    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('app_theme_mode'), 'dark');
  });

  test('restores a previously stored mode on build', () async {
    SharedPreferences.setMockInitialValues({'app_theme_mode': 'light'});
    final container = ProviderContainer();
    addTearDown(container.dispose);

    container.read(themeControllerProvider); // trigger build → async restore
    await Future<void>.delayed(Duration.zero);

    expect(container.read(themeControllerProvider), ThemeMode.light);
  });

  test('set(system) clears the stored override', () async {
    SharedPreferences.setMockInitialValues({'app_theme_mode': 'dark'});
    final container = ProviderContainer();
    addTearDown(container.dispose);

    await container
        .read(themeControllerProvider.notifier)
        .set(ThemeMode.system);

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('app_theme_mode'), isNull);
  });
}
