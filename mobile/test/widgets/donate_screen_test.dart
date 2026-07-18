import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/util/url_launcher_service.dart';
import 'package:mawa3id/features/donations/data/donation_models.dart';
import 'package:mawa3id/features/donations/data/donation_repository.dart';
import 'package:mawa3id/features/donations/state/donations_providers.dart';
import 'package:mawa3id/features/donations/ui/donate_screen.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockDonationRepository extends Mock implements DonationRepository {}

DonationConfig _config({
  bool card = true,
  bool konnect = false,
  String patreon = '',
  String rib = '',
}) =>
    DonationConfig(
      cardEnabled: card,
      currency: 'usd',
      minAmountMinor: 100,
      patreonUrl: patreon,
      konnectEnabled: konnect,
      konnectCurrency: 'tnd',
      ribNumber: rib,
      ribAccountHolder: rib.isEmpty ? '' : 'Assoc',
    );

void main() {
  setUpAll(() {
    registerFallbackValue(Uri.parse('https://x'));
    registerFallbackValue(DonationProvider.stripe);
  });

  setUp(() => SharedPreferences.setMockInitialValues({}));

  List<Override> overrides({
    required DonationConfig config,
    DonationRepository? repo,
    List<Uri>? launched,
  }) =>
      [
        donationConfigProvider.overrideWith((ref) async => config),
        myDonationsProvider.overrideWith((ref) async => const <Donation>[]),
        if (repo != null) donationRepositoryProvider.overrideWithValue(repo),
        if (launched != null)
          urlLauncherProvider.overrideWithValue((url) async {
            launched.add(url);
            return true;
          }),
      ];

  testWidgets('shows only the configured methods (Patreon-only)',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(const DonateScreen(),
        overrides: overrides(
            config: _config(card: false, patreon: 'https://patreon.com/m'))));
    await tester.pumpAndSettle();

    expect(find.text('Support on Patreon'), findsOneWidget);
    expect(find.text('Donate by card'), findsNothing);
  });

  testWidgets('card donation creates and opens the checkout url',
      (tester) async {
    final repo = _MockDonationRepository();
    final launched = <Uri>[];
    when(() => repo.create(
          amountMinor: any(named: 'amountMinor'),
          provider: any(named: 'provider'),
          currency: any(named: 'currency'),
          donorName: any(named: 'donorName'),
          message: any(named: 'message'),
        )).thenAnswer((_) async => Donation(
          id: 1,
          amountMinor: 1000,
          currency: 'usd',
          status: DonationStatus.pending,
          provider: DonationProvider.stripe,
          checkoutUrl: 'https://pay.stripe/cs_1',
          createdAt: DateTime.utc(2026, 7, 19),
        ));

    await tester.pumpWidget(wrapWithApp(const DonateScreen(),
        overrides: overrides(
            config: _config(), repo: repo, launched: launched)));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Donate by card'));
    await tester.pumpAndSettle();

    final captured = verify(() => repo.create(
          amountMinor: captureAny(named: 'amountMinor'),
          provider: captureAny(named: 'provider'),
          currency: any(named: 'currency'),
          donorName: any(named: 'donorName'),
          message: any(named: 'message'),
        )).captured;
    expect(captured[0], 1000); // 10 * 100 (cents)
    expect(captured[1], DonationProvider.stripe);
    expect(launched.single.toString(), 'https://pay.stripe/cs_1');
  });

  testWidgets('RIB details render with a record-transfer action',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(const DonateScreen(),
        overrides:
            overrides(config: _config(card: false, rib: '08 001 123'))));
    await tester.pumpAndSettle();

    expect(find.text('Bank transfer (RIB)'), findsOneWidget);
    expect(find.text('08 001 123'), findsOneWidget);
    expect(find.text("I've made the transfer"), findsOneWidget);
  });
}
