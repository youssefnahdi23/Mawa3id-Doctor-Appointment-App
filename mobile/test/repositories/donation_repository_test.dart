import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/donations/data/donation_models.dart';
import 'package:mawa3id/features/donations/data/donation_repository.dart';
import 'package:mocktail/mocktail.dart';

class _MockDio extends Mock implements Dio {}

Response<T> _ok<T>(T data) =>
    Response<T>(requestOptions: RequestOptions(), data: data, statusCode: 200);

void main() {
  late _MockDio dio;
  late DonationRepository repository;

  setUp(() {
    dio = _MockDio();
    repository = DonationRepository(dio);
  });

  test('config() parses all method fields', () async {
    when(() => dio.get<Map<String, dynamic>>(any())).thenAnswer((_) async => _ok({
          'cardEnabled': true,
          'currency': 'usd',
          'minAmountMinor': 100,
          'patreonUrl': 'https://patreon.com/mawa3id',
          'konnectEnabled': true,
          'konnectCurrency': 'tnd',
          'ribAccountHolder': 'Assoc',
          'ribBankName': 'BIAT',
          'ribNumber': '08 001 123',
          'ribCurrency': 'tnd',
        }));

    final config = await repository.config();

    expect(config.cardEnabled, isTrue);
    expect(config.konnectEnabled, isTrue);
    expect(config.ribEnabled, isTrue);
    expect(config.patreonEnabled, isTrue);
    expect(config.ribNumber, '08 001 123');
    verify(() => dio.get<Map<String, dynamic>>('/api/donations/config')).called(1);
  });

  test('create() posts amount + provider and parses the checkout url', () async {
    when(() => dio.post<Map<String, dynamic>>(any(), data: any(named: 'data')))
        .thenAnswer((_) async => _ok({
              'id': 1,
              'amountMinor': 5000,
              'currency': 'tnd',
              'status': 'PENDING',
              'provider': 'KONNECT',
              'checkoutUrl': 'https://konnect/pay/1',
              'createdAt': '2026-07-19T10:00:00Z',
            }));

    final donation = await repository.create(
      amountMinor: 5000,
      provider: DonationProvider.konnect,
      currency: 'tnd',
    );

    expect(donation.provider, DonationProvider.konnect);
    expect(donation.checkoutUrl, 'https://konnect/pay/1');
    final captured = verify(() => dio.post<Map<String, dynamic>>(
        captureAny(), data: captureAny(named: 'data'))).captured;
    expect(captured[0], '/api/donations');
    expect(captured[1], {
      'amountMinor': 5000,
      'provider': 'KONNECT',
      'currency': 'tnd',
    });
  });

  test('mine() reads the history list', () async {
    when(() => dio.get<List<dynamic>>(any())).thenAnswer((_) async => _ok([
          {
            'id': 2,
            'amountMinor': 1000,
            'currency': 'usd',
            'status': 'SUCCEEDED',
            'provider': 'STRIPE',
            'createdAt': '2026-07-18T09:00:00Z',
          }
        ]));

    final list = await repository.mine();

    expect(list.single.status, DonationStatus.succeeded);
    expect(list.single.checkoutUrl, isNull);
    verify(() => dio.get<List<dynamic>>('/api/donations/me')).called(1);
  });
}
