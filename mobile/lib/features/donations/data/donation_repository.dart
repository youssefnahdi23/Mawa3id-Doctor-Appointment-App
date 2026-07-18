import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/dio_client.dart';
import 'donation_models.dart';

final donationRepositoryProvider =
    Provider<DonationRepository>((ref) => DonationRepository(ref.watch(dioProvider)));

class DonationRepository {
  DonationRepository(this._dio);

  final Dio _dio;

  /// Which donation methods are available and their details.
  Future<DonationConfig> config() {
    return apiCall(() async {
      final response =
          await _dio.get<Map<String, dynamic>>('/api/donations/config');
      return DonationConfig.fromJson(response.data!);
    });
  }

  /// Start a donation on [provider]. For Stripe/Konnect the returned [Donation]
  /// carries a `checkoutUrl` to open; for RIB it is a PENDING record only.
  Future<Donation> create({
    required int amountMinor,
    required DonationProvider provider,
    String? currency,
    String? donorName,
    String? message,
  }) {
    return apiCall(() async {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/donations',
        data: {
          'amountMinor': amountMinor,
          'provider': provider.wireName,
          if (currency != null && currency.isNotEmpty) 'currency': currency,
          if (donorName != null && donorName.isNotEmpty) 'donorName': donorName,
          if (message != null && message.isNotEmpty) 'message': message,
        },
      );
      return Donation.fromJson(response.data!);
    });
  }

  /// The signed-in user's donation history, newest first.
  Future<List<Donation>> mine() {
    return apiCall(() async {
      final response = await _dio.get<List<dynamic>>('/api/donations/me');
      return response.data!
          .map((item) => Donation.fromJson(item as Map<String, dynamic>))
          .toList();
    });
  }
}
