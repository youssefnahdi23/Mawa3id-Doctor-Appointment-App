import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/community/data/feedback_models.dart';
import 'package:mawa3id/features/community/data/feedback_repository.dart';
import 'package:mawa3id/features/community/ui/feedback_screen.dart';
import 'package:mocktail/mocktail.dart';

import 'helpers.dart';

class _MockFeedbackRepository extends Mock implements FeedbackRepository {}

void main() {
  setUpAll(() => registerFallbackValue(FeedbackCategory.suggestion));

  testWidgets('renders the form fields', (tester) async {
    await tester.pumpWidget(wrapWithApp(const FeedbackScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Category'), findsOneWidget);
    expect(find.text('Your message'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Send'), findsOneWidget);
  });

  testWidgets('blank message blocks submission', (tester) async {
    final repo = _MockFeedbackRepository();
    await tester.pumpWidget(wrapWithApp(
      const FeedbackScreen(),
      overrides: [feedbackRepositoryProvider.overrideWithValue(repo)],
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(FilledButton, 'Send'));
    await tester.pumpAndSettle();

    expect(find.text('This field is required'), findsOneWidget);
    verifyNever(() => repo.submit(
        category: any(named: 'category'), message: any(named: 'message')));
  });
}
