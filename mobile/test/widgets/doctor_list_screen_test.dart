import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/core/models/page_response.dart';
import 'package:mawa3id/features/doctors/data/doctor_models.dart';
import 'package:mawa3id/features/doctors/data/doctor_repository.dart';
import 'package:mawa3id/features/doctors/ui/doctor_list_screen.dart';
import 'package:mocktail/mocktail.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'helpers.dart';

class _MockDoctorRepository extends Mock implements DoctorRepository {}

PageResponse<DoctorSummary> _page(List<DoctorSummary> items) => PageResponse(
      content: items,
      page: PageMeta(
          size: 20,
          number: 0,
          totalElements: items.length,
          totalPages: 1),
    );

const _amal = DoctorSummary(
    userId: 3,
    name: 'Dr. Amal',
    specialtyName: 'Cardiology',
    ratingAverage: 4.5,
    ratingCount: 12);

const _bilel = DoctorSummary(
    userId: 4,
    name: 'Dr. Bilel',
    specialtyName: 'Dermatology',
    ratingAverage: 0,
    ratingCount: 0);

void main() {
  late _MockDoctorRepository repository;

  // The controller's first-page fetch passes exactly these named args.
  void stubList(List<DoctorSummary> items) {
    when(() => repository.list(
            specialtyId: any(named: 'specialtyId'),
            query: any(named: 'query')))
        .thenAnswer((_) async => _page(items));
  }

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    repository = _MockDoctorRepository();
    when(() => repository.specialties()).thenAnswer((_) async => const [
          SpecialtyResponse(id: 1, name: 'Cardiology'),
        ]);
  });

  testWidgets('renders fetched doctors with their ratings', (tester) async {
    stubList([_amal, _bilel]);

    await tester.pumpWidget(wrapWithApp(const DoctorListScreen(), overrides: [
      doctorRepositoryProvider.overrideWithValue(repository),
    ]));
    await tester.pumpAndSettle();

    expect(find.text('Dr. Amal'), findsOneWidget);
    expect(find.text('Dr. Bilel'), findsOneWidget);
    expect(find.textContaining('4.5'), findsOneWidget);
  });

  testWidgets('selecting a specialty chip reloads with the filter',
      (tester) async {
    stubList([_amal, _bilel]);

    await tester.pumpWidget(wrapWithApp(const DoctorListScreen(), overrides: [
      doctorRepositoryProvider.overrideWithValue(repository),
    ]));
    await tester.pumpAndSettle();

    stubList([_amal]);
    // 'Cardiology' also appears on doctor cards; target the chip explicitly.
    await tester.tap(find.widgetWithText(FilterChip, 'Cardiology'));
    await tester.pumpAndSettle();

    verify(() =>
            repository.list(specialtyId: 1, query: any(named: 'query')))
        .called(1);
    expect(find.text('Dr. Bilel'), findsNothing);
  });

  testWidgets('shows the empty state when nothing matches', (tester) async {
    stubList([]);

    await tester.pumpWidget(wrapWithApp(const DoctorListScreen(), overrides: [
      doctorRepositoryProvider.overrideWithValue(repository),
    ]));
    await tester.pumpAndSettle();

    expect(find.text('No doctors found'), findsOneWidget);
  });
}
