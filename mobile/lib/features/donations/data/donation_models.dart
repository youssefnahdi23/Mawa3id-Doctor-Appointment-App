import 'package:json_annotation/json_annotation.dart';

part 'donation_models.g.dart';

/// Payment channel for a donation. Values match the backend `DonationProvider`.
enum DonationProvider {
  @JsonValue('STRIPE')
  stripe,
  @JsonValue('KONNECT')
  konnect,
  @JsonValue('RIB')
  rib,
}

extension DonationProviderWire on DonationProvider {
  String get wireName => switch (this) {
        DonationProvider.stripe => 'STRIPE',
        DonationProvider.konnect => 'KONNECT',
        DonationProvider.rib => 'RIB',
      };
}

/// Lifecycle of a donation. Matches the backend `DonationStatus`.
enum DonationStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('SUCCEEDED')
  succeeded,
  @JsonValue('FAILED')
  failed,
  @JsonValue('EXPIRED')
  expired,
}

/// Which donation methods are available and the details to surface each one
/// (`GET /api/donations/config`). String fields are empty when not configured.
@JsonSerializable(createToJson: false)
class DonationConfig {
  const DonationConfig({
    required this.cardEnabled,
    required this.currency,
    required this.minAmountMinor,
    required this.patreonUrl,
    this.konnectEnabled = false,
    this.konnectCurrency = 'tnd',
    this.ribAccountHolder = '',
    this.ribBankName = '',
    this.ribNumber = '',
    this.ribCurrency = 'tnd',
  });

  factory DonationConfig.fromJson(Map<String, dynamic> json) =>
      _$DonationConfigFromJson(json);

  /// International card via Stripe; [currency]/[minAmountMinor] apply to it.
  final bool cardEnabled;
  final String currency;
  final int minAmountMinor;

  /// Recurring-support link; empty when not configured.
  final String patreonUrl;

  /// Tunisian aggregator (D17 / Flouci / local cards); amounts in [konnectCurrency].
  @JsonKey(defaultValue: false)
  final bool konnectEnabled;
  @JsonKey(defaultValue: 'tnd')
  final String konnectCurrency;

  /// Manual bank-transfer details; [ribNumber] empty means the method is off.
  @JsonKey(defaultValue: '')
  final String ribAccountHolder;
  @JsonKey(defaultValue: '')
  final String ribBankName;
  @JsonKey(defaultValue: '')
  final String ribNumber;
  @JsonKey(defaultValue: 'tnd')
  final String ribCurrency;

  bool get patreonEnabled => patreonUrl.isNotEmpty;
  bool get ribEnabled => ribNumber.isNotEmpty;

  /// True when no donation method is currently available.
  bool get noneAvailable =>
      !cardEnabled && !konnectEnabled && !patreonEnabled && !ribEnabled;
}

/// A donation record. `checkoutUrl` is only present on creation (the hosted
/// payment page to open); it is null in history listings.
@JsonSerializable(createToJson: false)
class Donation {
  const Donation({
    required this.id,
    required this.amountMinor,
    required this.currency,
    required this.status,
    required this.provider,
    this.message,
    this.checkoutUrl,
    required this.createdAt,
  });

  factory Donation.fromJson(Map<String, dynamic> json) =>
      _$DonationFromJson(json);

  final int id;
  final int amountMinor;
  final String currency;
  final DonationStatus status;
  final DonationProvider provider;
  final String? message;
  final String? checkoutUrl;
  final DateTime createdAt;
}
