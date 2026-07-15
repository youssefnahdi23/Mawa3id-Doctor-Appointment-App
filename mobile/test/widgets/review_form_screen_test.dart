import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/reviews/data/review_repository.dart';
import 'package:mawa3id/features/reviews/ui/review_form_screen.dart';
import 'package:mocktail/mocktail.dart';

import 'helpers.dart';

class _MockReviewRepository extends Mock implements ReviewRepository {}

void main() {
  setUpAll(() {
    registerFallbackValue(const ReviewRequest(rating: 1));
  });

  late _MockReviewRepository repository;

  setUp(() {
    repository = _MockReviewRepository();
    // No review yet → create mode.
    when(() => repository.get(any())).thenAnswer((_) async => null);
  });

  Widget subject() => wrapWithApp(
        const ReviewFormScreen(appointmentId: 11, doctorId: 3),
        overrides: [reviewRepositoryProvider.overrideWithValue(repository)],
      );

  testWidgets('renders five empty stars', (tester) async {
    await tester.pumpWidget(subject());
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.star_border), findsNWidgets(5));
    expect(find.byIcon(Icons.star), findsNothing);
  });

  testWidgets('tapping a star fills it and the ones before it',
      (tester) async {
    await tester.pumpWidget(subject());
    await tester.pumpAndSettle();

    // Tap the fourth star.
    await tester.tap(find.byIcon(Icons.star_border).at(3));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.star), findsNWidgets(4));
    expect(find.byIcon(Icons.star_border), findsNWidgets(1));
  });

  testWidgets('submitting without a rating is blocked', (tester) async {
    await tester.pumpWidget(subject());
    await tester.pumpAndSettle();

    await tester.tap(find.text('Submit review'));
    await tester.pumpAndSettle();

    expect(find.text('Tap a star to rate'), findsOneWidget);
    verifyNever(() => repository.create(any(), any()));
  });
}
