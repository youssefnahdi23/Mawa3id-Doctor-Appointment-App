import 'package:flutter/widgets.dart';

/// Design tokens for the "Clinical Vitality" system.
///
/// Spacing follows an 8px spatial grid on a 4px unit; radii follow the
/// system's soft-geometry shape scale. Use these instead of scattering magic
/// numbers so the UI keeps a consistent clinical rhythm.
class AppSpacing {
  AppSpacing._();

  /// 4px base unit (micro-adjustments, icon-to-text gaps).
  static const double xs = 4;

  /// 8px — tight stacks, chip gaps.
  static const double sm = 8;

  /// 12px — comfortable list/card internal gaps.
  static const double md = 12;

  /// 16px — default card padding, screen safe-area margin, vertical rhythm.
  static const double lg = 16;

  /// 24px — section separation.
  static const double xl = 24;

  /// 32px — large section / hero separation.
  static const double xxl = 32;

  /// Horizontal safe-area margin on mobile.
  static const double screen = 16;
}

/// Corner radii for the rounded, approachable shape language.
class AppRadii {
  AppRadii._();

  /// 4px — micro elements.
  static const double sm = 4;

  /// 6px — specialty / metadata badges.
  static const double badge = 6;

  /// 8px — default rounding.
  static const double base = 8;

  /// 10px — status chips.
  static const double chip = 10;

  /// 12px — inputs.
  static const double input = 12;

  /// 14px — buttons.
  static const double button = 14;

  /// 16px — cards / large containers.
  static const double card = 16;

  /// 24px — sheets / dialogs.
  static const double xl = 24;

  /// Fully pill-shaped.
  static const double full = 999;

  static const BorderRadius badgeR = BorderRadius.all(Radius.circular(badge));
  static const BorderRadius chipR = BorderRadius.all(Radius.circular(chip));
  static const BorderRadius inputR = BorderRadius.all(Radius.circular(input));
  static const BorderRadius buttonR = BorderRadius.all(Radius.circular(button));
  static const BorderRadius cardR = BorderRadius.all(Radius.circular(card));
  static const BorderRadius xlR = BorderRadius.all(Radius.circular(xl));
}
