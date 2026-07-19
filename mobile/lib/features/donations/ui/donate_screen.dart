import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/util/url_launcher_service.dart';
import '../../../core/widgets/empty_state.dart';
import '../../../core/widgets/error_view.dart';
import '../data/donation_models.dart';
import '../data/donation_repository.dart';
import '../state/donations_providers.dart';

/// TND is quoted in millimes (3 decimals); most other currencies in cents (2).
int _minorFactor(String currency) => currency.toLowerCase() == 'tnd' ? 1000 : 100;

String _formatMinor(int amountMinor, String currency) {
  final value = amountMinor / _minorFactor(currency);
  final formatted =
      NumberFormat.currency(name: currency.toUpperCase(), symbol: '')
          .format(value)
          .trim();
  return '$formatted ${currency.toUpperCase()}';
}

/// "Support Mawa3id" — the donation hub. Shows only the methods the backend
/// reports as configured (card via Stripe, Konnect for D17/Flouci/local cards,
/// Patreon, and RIB bank transfer), plus the donor's history.
class DonateScreen extends ConsumerStatefulWidget {
  const DonateScreen({super.key});

  @override
  ConsumerState<DonateScreen> createState() => _DonateScreenState();
}

class _DonateScreenState extends ConsumerState<DonateScreen> {
  final _amount = TextEditingController(text: '10');
  bool _busy = false;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  double? get _amountValue {
    final v = double.tryParse(_amount.text.trim().replaceAll(',', '.'));
    return (v == null || v <= 0) ? null : v;
  }

  int? _amountMinor(String currency) {
    final v = _amountValue;
    if (v == null) return null;
    return (v * _minorFactor(currency)).round();
  }

  Future<void> _pay(DonationProvider provider, String currency) async {
    final l10n = context.l10n;
    final minor = _amountMinor(currency);
    if (minor == null) {
      _snack(l10n.donationInvalidAmount);
      return;
    }
    setState(() => _busy = true);
    try {
      final donation = await ref.read(donationRepositoryProvider).create(
            amountMinor: minor,
            provider: provider,
            currency: currency,
          );
      final url = donation.checkoutUrl;
      if (url == null) {
        _snack(l10n.errorGeneric);
        return;
      }
      final opened = await ref.read(urlLauncherProvider)(Uri.parse(url));
      if (!mounted) return;
      _snack(opened ? l10n.donationCompleteInBrowser : l10n.donationCouldNotOpen);
      ref.invalidate(myDonationsProvider);
    } catch (e) {
      if (mounted) _snack(localizedErrorMessage(context, e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _openLink(String url) async {
    final opened = await ref.read(urlLauncherProvider)(Uri.parse(url));
    if (!opened && mounted) _snack(context.l10n.donationCouldNotOpen);
  }

  Future<void> _recordRib(String currency) async {
    final l10n = context.l10n;
    final minor = _amountMinor(currency);
    if (minor == null) {
      _snack(l10n.donationInvalidAmount);
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(donationRepositoryProvider).create(
            amountMinor: minor,
            provider: DonationProvider.rib,
            currency: currency,
          );
      if (!mounted) return;
      _snack(l10n.donationRibRecorded);
      ref.invalidate(myDonationsProvider);
    } catch (e) {
      if (mounted) _snack(localizedErrorMessage(context, e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _snack(String message) => ScaffoldMessenger.of(context)
      .showSnackBar(SnackBar(content: Text(message)));

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final config = ref.watch(donationConfigProvider);
    return Scaffold(
      appBar: AppBar(title: Text(l10n.supportMawa3id)),
      body: config.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(donationConfigProvider),
        ),
        data: (cfg) => cfg.noneAvailable
            ? EmptyState(
                icon: Icons.volunteer_activism_outlined,
                title: l10n.donationsUnavailable,
              )
            : _content(cfg),
      ),
    );
  }

  Widget _content(DonationConfig cfg) {
    final l10n = context.l10n;
    final showAmount = cfg.cardEnabled || cfg.konnectEnabled || cfg.ribEnabled;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(l10n.donateIntro, style: Theme.of(context).textTheme.bodyLarge),
        const SizedBox(height: 16),
        if (showAmount) _amountCard(),
        if (cfg.cardEnabled)
          _methodTile(
            icon: Icons.credit_card,
            title: l10n.donateByCard,
            subtitle: cfg.currency.toUpperCase(),
            onTap: _busy ? null : () => _pay(DonationProvider.stripe, cfg.currency),
          ),
        if (cfg.konnectEnabled)
          _methodTile(
            icon: Icons.account_balance_wallet_outlined,
            title: l10n.donateWithKonnect,
            subtitle: cfg.konnectCurrency.toUpperCase(),
            onTap:
                _busy ? null : () => _pay(DonationProvider.konnect, cfg.konnectCurrency),
          ),
        if (cfg.patreonEnabled)
          _methodTile(
            icon: Icons.favorite_outline,
            title: l10n.donateViaPatreon,
            subtitle: l10n.donatePatreonSubtitle,
            onTap: () => _openLink(cfg.patreonUrl),
          ),
        if (cfg.ribEnabled) _ribCard(cfg),
        const SizedBox(height: 24),
        Text(l10n.donationHistory,
            style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        _history(),
      ],
    );
  }

  Widget _amountCard() {
    final l10n = context.l10n;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.donationAmount,
                style: Theme.of(context).textTheme.labelLarge),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                for (final preset in [5, 10, 20, 50])
                  ActionChip(
                    label: Text('$preset'),
                    onPressed: () =>
                        setState(() => _amount.text = '$preset'),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _amount,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
              ],
              decoration: InputDecoration(labelText: l10n.donationAmount),
              onChanged: (_) => setState(() {}),
            ),
          ],
        ),
      ),
    );
  }

  Widget _methodTile({
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback? onTap,
  }) {
    return Card(
      child: ListTile(
        leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
        title: Text(title),
        subtitle: Text(subtitle),
        trailing: _busy
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2))
            : const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }

  Widget _ribCard(DonationConfig cfg) {
    final l10n = context.l10n;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.account_balance_outlined,
                    color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 12),
                Text(l10n.donateByBankTransfer,
                    style: Theme.of(context).textTheme.titleSmall),
              ],
            ),
            const SizedBox(height: 12),
            if (cfg.ribAccountHolder.isNotEmpty)
              _ribRow(l10n.ribAccountHolder, cfg.ribAccountHolder),
            if (cfg.ribBankName.isNotEmpty)
              _ribRow(l10n.ribBank, cfg.ribBankName),
            _ribRow(l10n.ribNumber, cfg.ribNumber, copyable: true),
            const SizedBox(height: 12),
            Text(l10n.ribInstructions,
                style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed:
                  _busy ? null : () => _recordRib(cfg.ribCurrency),
              icon: const Icon(Icons.done),
              label: Text(l10n.ribMarkTransferred),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ribRow(String label, String value, {bool copyable = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label,
                style: TextStyle(
                    color: Theme.of(context).colorScheme.outline)),
          ),
          Expanded(child: Text(value)),
          if (copyable)
            InkWell(
              onTap: () {
                Clipboard.setData(ClipboardData(text: value));
                _snack(context.l10n.copied);
              },
              child: const Padding(
                padding: EdgeInsets.only(left: 8),
                child: Icon(Icons.copy, size: 18),
              ),
            ),
        ],
      ),
    );
  }

  Widget _history() {
    final l10n = context.l10n;
    final donations = ref.watch(myDonationsProvider);
    return donations.when(
      loading: () => const Center(
          child: Padding(
              padding: EdgeInsets.all(16),
              child: CircularProgressIndicator())),
      error: (error, _) => ErrorView(
        error: error,
        onRetry: () => ref.invalidate(myDonationsProvider),
      ),
      data: (list) {
        if (list.isEmpty) {
          return EmptyState(
            icon: Icons.receipt_long_outlined,
            title: l10n.donationHistoryEmpty,
          );
        }
        return Column(
          children: [
            for (final d in list)
              Card(
                child: ListTile(
                  title: Text(_formatMinor(d.amountMinor, d.currency)),
                  subtitle: Text(DateFormat.yMMMd(
                          Localizations.localeOf(context).toString())
                      .format(d.createdAt.toLocal())),
                  trailing: _StatusChip(status: d.status),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final DonationStatus status;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final colors = Theme.of(context).colorScheme;
    final (label, color) = switch (status) {
      DonationStatus.succeeded => (l10n.donationStatusSucceeded, Colors.green),
      DonationStatus.pending => (l10n.donationStatusPending, colors.tertiary),
      DonationStatus.failed => (l10n.donationStatusFailed, colors.error),
      DonationStatus.expired => (l10n.donationStatusExpired, colors.outline),
    };
    return Chip(
      label: Text(label, style: TextStyle(fontSize: 12, color: color)),
      visualDensity: VisualDensity.compact,
      side: BorderSide(color: color.withValues(alpha: 0.5)),
    );
  }
}
