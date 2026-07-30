import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mawa3id/features/auth/data/auth_models.dart';
import 'package:mawa3id/features/auth/state/session_controller.dart';
import 'package:mawa3id/features/community/ui/community_screen.dart';

import 'helpers.dart';

void main() {
  const session = Session(
    userId: 1,
    username: 'sami',
    email: 'a@b.c',
    role: UserRole.patient,
  );

  testWidgets('signed-in user sees feedback, contribute, and support',
      (tester) async {
    await tester.pumpWidget(wrapWithApp(
      const CommunityScreen(),
      overrides: [
        sessionControllerProvider
            .overrideWith(() => FakeSessionController(session)),
      ],
    ));
    await tester.pumpAndSettle();

    expect(find.text('Send feedback'), findsOneWidget);
    expect(find.text('View source on GitHub'), findsOneWidget);
    expect(find.text('Support Mawa3id'), findsOneWidget);
  });

  testWidgets('logged-out visitor sees only the contribute section',
      (tester) async {
    // wrapWithApp's default fake session is null (logged out).
    await tester.pumpWidget(wrapWithApp(const CommunityScreen()));
    await tester.pumpAndSettle();

    expect(find.text('View source on GitHub'), findsOneWidget);
    expect(find.text('Send feedback'), findsNothing);
    expect(find.text('Support Mawa3id'), findsNothing);
  });
}
