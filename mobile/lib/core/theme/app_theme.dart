import 'package:flutter/material.dart';

/// The Mawa3id design system: a single source of truth for the light and dark
/// themes. Built from one medical-green seed so both schemes stay in harmony;
/// component themes give forms, cards and navigation a consistent, rounded,
/// calm look across every screen.
class AppTheme {
  AppTheme._();

  /// Medical green — the brand seed. Material 3 derives the full tonal palette
  /// (primary/secondary/surfaces) from this for both light and dark.
  static const Color seed = Color(0xFF1E8E5A);

  static ThemeData light() => _build(Brightness.light);

  static ThemeData dark() => _build(Brightness.dark);

  static ThemeData _build(Brightness brightness) {
    final colors = ColorScheme.fromSeed(
      seedColor: seed,
      brightness: brightness,
    );
    final base = ThemeData(useMaterial3: true, colorScheme: colors);

    return base.copyWith(
      scaffoldBackgroundColor: colors.surface,
      appBarTheme: AppBarTheme(
        backgroundColor: colors.surface,
        foregroundColor: colors.onSurface,
        surfaceTintColor: colors.surfaceTint,
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 2,
        titleTextStyle: base.textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w600,
          color: colors.onSurface,
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: colors.surfaceContainerLow,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        margin: const EdgeInsets.symmetric(vertical: 6),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colors.surfaceContainerHighest.withValues(alpha: 0.4),
        border: _inputBorder(colors.outlineVariant),
        enabledBorder: _inputBorder(colors.outlineVariant),
        focusedBorder: _inputBorder(colors.primary, width: 2),
        errorBorder: _inputBorder(colors.error),
        focusedErrorBorder: _inputBorder(colors.error, width: 2),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(0, 52),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14)),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(0, 52),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14)),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: colors.surface,
        indicatorColor: colors.primaryContainer,
        elevation: 3,
        labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      dialogTheme: DialogThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      chipTheme: base.chipTheme.copyWith(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  static OutlineInputBorder _inputBorder(Color color, {double width = 1}) {
    return OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: color, width: width),
    );
  }
}
