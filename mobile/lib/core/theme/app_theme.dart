import 'package:flutter/material.dart';

import 'app_tokens.dart';

/// The Mawa3id design system — **"Clinical Vitality"**.
///
/// A single source of truth for the light and dark themes: a botanical emerald
/// palette on a crisp clinical off-white, Manrope type across every role, and
/// soft, flat, tonally-layered components. The light scheme maps the design
/// system's named color roles exactly; the dark scheme is derived from the same
/// emerald seed so both stay in harmony.
class AppTheme {
  AppTheme._();

  /// Emerald brand seed (`#1E8E5A`) — drives the dark tonal palette.
  static const Color seed = Color(0xFF1E8E5A);

  /// Vivid emerald used for primary actions in the mockups.
  static const Color brand = Color(0xFF0D8552);

  static const String fontFamily = 'Manrope';

  // ── Clinical Vitality named colors (light) ──────────────────────────────
  static const ColorScheme _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF006A40),
    onPrimary: Color(0xFFFFFFFF),
    primaryContainer: Color(0xFF0D8552),
    onPrimaryContainer: Color(0xFFF6FFF5),
    secondary: Color(0xFF4D6356),
    onSecondary: Color(0xFFFFFFFF),
    secondaryContainer: Color(0xFFCDE6D5),
    onSecondaryContainer: Color(0xFF51685A),
    tertiary: Color(0xFF3B6171),
    onTertiary: Color(0xFFFFFFFF),
    tertiaryContainer: Color(0xFF547A8A),
    onTertiaryContainer: Color(0xFFFBFDFF),
    error: Color(0xFFBA1A1A),
    onError: Color(0xFFFFFFFF),
    errorContainer: Color(0xFFFFDAD6),
    onErrorContainer: Color(0xFF93000A),
    surface: Color(0xFFF5FBF5),
    onSurface: Color(0xFF171D1A),
    onSurfaceVariant: Color(0xFF3E4941),
    surfaceContainerLowest: Color(0xFFFFFFFF),
    surfaceContainerLow: Color(0xFFEFF5F0),
    surfaceContainer: Color(0xFFE9EFEA),
    surfaceContainerHigh: Color(0xFFE4EAE4),
    surfaceContainerHighest: Color(0xFFDEE4DF),
    surfaceDim: Color(0xFFD5DCD6),
    surfaceBright: Color(0xFFF5FBF5),
    outline: Color(0xFF6E7A70),
    outlineVariant: Color(0xFFBDCABE),
    inverseSurface: Color(0xFF2C322E),
    onInverseSurface: Color(0xFFECF2ED),
    inversePrimary: Color(0xFF74DB9F),
    surfaceTint: Color(0xFF006D41),
    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
  );

  static final ColorScheme _darkScheme = ColorScheme.fromSeed(
    seedColor: seed,
    brightness: Brightness.dark,
  );

  static ThemeData light() => _build(_lightScheme);

  static ThemeData dark() => _build(_darkScheme);

  static ThemeData _build(ColorScheme colors) {
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: colors,
      fontFamily: fontFamily,
    );
    final text = _textTheme(base.textTheme, colors.onSurface);

    return base.copyWith(
      textTheme: text,
      scaffoldBackgroundColor: colors.surface,
      appBarTheme: AppBarTheme(
        backgroundColor: colors.surface,
        foregroundColor: colors.onSurface,
        surfaceTintColor: Colors.transparent,
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0.5,
        titleTextStyle: text.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: colors.onSurface,
        ),
      ),
      // Flat, tonal cards on surface-container-low with strict 16px corners.
      cardTheme: CardThemeData(
        elevation: 0,
        color: colors.surfaceContainerLow,
        surfaceTintColor: Colors.transparent,
        clipBehavior: Clip.antiAlias,
        shape: const RoundedRectangleBorder(borderRadius: AppRadii.cardR),
        margin: EdgeInsets.zero,
      ),
      // Filled inputs with a 2px primary border only on focus.
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colors.surfaceContainerHighest.withValues(alpha: 0.5),
        border: _inputBorder(Colors.transparent),
        enabledBorder: _inputBorder(Colors.transparent),
        focusedBorder: _inputBorder(colors.primary, width: 2),
        errorBorder: _inputBorder(colors.error),
        focusedErrorBorder: _inputBorder(colors.error, width: 2),
        hintStyle: TextStyle(color: colors.onSurfaceVariant),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: 14),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(0, 52),
          shape:
              const RoundedRectangleBorder(borderRadius: AppRadii.buttonR),
          textStyle: const TextStyle(
              fontFamily: fontFamily, fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size(0, 52),
          elevation: 0,
          shape:
              const RoundedRectangleBorder(borderRadius: AppRadii.buttonR),
          textStyle: const TextStyle(
              fontFamily: fontFamily, fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(0, 52),
          side: BorderSide(color: colors.secondary),
          foregroundColor: colors.secondary,
          shape:
              const RoundedRectangleBorder(borderRadius: AppRadii.buttonR),
          textStyle: const TextStyle(
              fontFamily: fontFamily, fontSize: 16, fontWeight: FontWeight.w600),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          textStyle: const TextStyle(
              fontFamily: fontFamily, fontWeight: FontWeight.w600),
        ),
      ),
      // Pill active indicator; labels only for the selected item.
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: colors.surface,
        indicatorColor: colors.primaryContainer,
        surfaceTintColor: Colors.transparent,
        height: 68,
        elevation: 0,
        labelBehavior: NavigationDestinationLabelBehavior.onlyShowSelected,
        iconTheme: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return IconThemeData(
            color: selected ? colors.onPrimaryContainer : colors.onSurfaceVariant,
          );
        }),
        labelTextStyle: WidgetStateProperty.all(
          text.labelMedium?.copyWith(fontWeight: FontWeight.w600),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: colors.primary,
        foregroundColor: colors.onPrimary,
        elevation: 2,
        shape: const RoundedRectangleBorder(borderRadius: AppRadii.cardR),
      ),
      chipTheme: base.chipTheme.copyWith(
        shape:
            const RoundedRectangleBorder(borderRadius: AppRadii.chipR),
        side: BorderSide(color: colors.outlineVariant),
        labelStyle: text.labelLarge,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape:
            const RoundedRectangleBorder(borderRadius: AppRadii.chipR),
      ),
      dialogTheme: const DialogThemeData(
        shape: RoundedRectangleBorder(borderRadius: AppRadii.xlR),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        shape: RoundedRectangleBorder(
          borderRadius:
              BorderRadius.vertical(top: Radius.circular(AppRadii.xl)),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: colors.outlineVariant,
        space: 1,
        thickness: 1,
      ),
    );
  }

  /// Applies Manrope weights per the Clinical Vitality type scale: bold
  /// headlines, semibold titles/labels, regular body.
  static TextTheme _textTheme(TextTheme base, Color onSurface) {
    TextStyle? h(TextStyle? s) =>
        s?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.2);
    TextStyle? t(TextStyle? s) => s?.copyWith(fontWeight: FontWeight.w600);
    TextStyle? l(TextStyle? s) => s?.copyWith(fontWeight: FontWeight.w600);
    return base
        .copyWith(
          displayLarge: h(base.displayLarge),
          displayMedium: h(base.displayMedium),
          displaySmall: h(base.displaySmall),
          headlineLarge: h(base.headlineLarge),
          headlineMedium: h(base.headlineMedium),
          headlineSmall: h(base.headlineSmall),
          titleLarge: h(base.titleLarge),
          titleMedium: t(base.titleMedium),
          titleSmall: t(base.titleSmall),
          labelLarge: l(base.labelLarge),
          labelMedium: l(base.labelMedium),
          labelSmall: l(base.labelSmall),
        )
        .apply(fontFamily: fontFamily);
  }

  static OutlineInputBorder _inputBorder(Color color, {double width = 1}) {
    return OutlineInputBorder(
      borderRadius: AppRadii.inputR,
      borderSide: BorderSide(color: color, width: width),
    );
  }
}
