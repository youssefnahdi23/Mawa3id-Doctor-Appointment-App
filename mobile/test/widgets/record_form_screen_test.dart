import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/records/data/record_models.dart';
import 'package:mawa3id/features/records/data/record_repository.dart';
import 'package:mawa3id/features/records/ui/record_form_screen.dart';
import 'package:mocktail/mocktail.dart';

import 'helpers.dart';

class _MockRecordRepository extends Mock implements RecordRepository {}

void main() {
  setUpAll(() {
    registerFallbackValue(const RecordRequest(diagnosis: 'x'));
  });

  late _MockRecordRepository repository;

  setUp(() {
    repository = _MockRecordRepository();
    // No record yet → create mode.
    when(() => repository.get(any())).thenAnswer((_) async => null);
  });

  testWidgets('shows the diagnosis field once loaded', (tester) async {
    await tester.pumpWidget(wrapWithApp(
      const RecordFormScreen(appointmentId: 11),
      overrides: [recordRepositoryProvider.overrideWithValue(repository)],
    ));
    await tester.pumpAndSettle();

    expect(find.text('Diagnosis'), findsOneWidget);
    expect(find.text('Save & complete visit'), findsOneWidget);
  });

  testWidgets('blocks submission when diagnosis is empty', (tester) async {
    await tester.pumpWidget(wrapWithApp(
      const RecordFormScreen(appointmentId: 11),
      overrides: [recordRepositoryProvider.overrideWithValue(repository)],
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Save & complete visit'));
    await tester.pumpAndSettle();

    expect(find.text('A diagnosis is required'), findsOneWidget);
    verifyNever(() => repository.create(any(), any()));
  });
}
