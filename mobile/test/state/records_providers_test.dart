import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/models/page_response.dart';
import 'package:mawa3id/features/records/data/record_models.dart';
import 'package:mawa3id/features/records/data/record_repository.dart';
import 'package:mawa3id/features/records/state/records_providers.dart';
import 'package:mocktail/mocktail.dart';

class _MockRecordRepository extends Mock implements RecordRepository {}

MedicalRecordResponse _record(int id) => MedicalRecordResponse(
      id: id,
      appointmentId: id,
      doctorId: 3,
      doctorName: 'Dr. Amal',
      patientId: 7,
      patientName: 'Sami',
      visitStart: DateTime(2026, 7, 14, 9, 30),
      diagnosis: 'Diagnosis $id',
      createdAt: DateTime.utc(2026, 7, 14, 10),
    );

PageResponse<MedicalRecordResponse> _page(
        List<MedicalRecordResponse> items, int number, int totalPages) =>
    PageResponse(
      content: items,
      page: PageMeta(
          size: 20, number: number, totalElements: 40, totalPages: totalPages),
    );

void main() {
  late _MockRecordRepository repository;

  setUp(() {
    repository = _MockRecordRepository();
  });

  ProviderContainer container() {
    final c = ProviderContainer(
      overrides: [recordRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(c.dispose);
    return c;
  }

  test('appointmentRecordProvider passes through a null (no record)', () async {
    when(() => repository.get(11)).thenAnswer((_) async => null);
    final c = container();

    final result = await c.read(appointmentRecordProvider(11).future);
    expect(result, isNull);
  });

  test('patientRecordsProvider loads the first page then appends more',
      () async {
    when(() => repository.mine(page: 0))
        .thenAnswer((_) async => _page([_record(1)], 0, 2));
    when(() => repository.mine(page: 1))
        .thenAnswer((_) async => _page([_record(2)], 1, 2));

    final c = container();
    final first = await c.read(patientRecordsProvider.future);
    expect(first.items, hasLength(1));
    expect(first.isLast, isFalse);

    await c.read(patientRecordsProvider.notifier).loadMore();
    final combined = c.read(patientRecordsProvider).requireValue;
    expect(combined.items.map((r) => r.id), [1, 2]);
    expect(combined.isLast, isTrue);
  });
}
