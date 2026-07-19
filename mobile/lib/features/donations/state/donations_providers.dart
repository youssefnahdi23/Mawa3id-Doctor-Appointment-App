import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/donation_models.dart';
import '../data/donation_repository.dart';

/// Available donation methods + details; loaded once for the Donate screen.
final donationConfigProvider = FutureProvider.autoDispose<DonationConfig>(
    (ref) => ref.watch(donationRepositoryProvider).config());

/// The signed-in user's donation history, newest first.
final myDonationsProvider = FutureProvider.autoDispose<List<Donation>>(
    (ref) => ref.watch(donationRepositoryProvider).mine());
