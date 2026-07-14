import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/auth_events.dart';
import '../../../core/network/dio_client.dart';
import '../../../core/storage/token_storage.dart';
import '../data/auth_models.dart';
import '../data/auth_repository.dart';

class Session {
  const Session({
    required this.userId,
    required this.email,
    required this.role,
  });

  final int userId;
  final String email;
  final UserRole role;
}

final sessionControllerProvider =
    AsyncNotifierProvider<SessionController, Session?>(SessionController.new);

/// Owns the signed-in state. `null` data means logged out; loading means the
/// boot restore is still running (the router shows the splash screen).
class SessionController extends AsyncNotifier<Session?> {
  @override
  Future<Session?> build() async {
    final forcedLogout = ref
        .watch(authEventsProvider)
        .onForcedLogout
        .listen((_) => logout());
    ref.onDispose(forcedLogout.cancel);

    final storage = ref.watch(tokenStorageProvider);
    final stored = await storage.read();
    if (stored == null) return null;
    if (stored.isExpired) {
      await storage.clear();
      return null;
    }
    try {
      final me = await ref.read(authRepositoryProvider).me();
      return Session(userId: me.userId, email: me.email, role: me.role);
    } on ApiException catch (e) {
      if (e.kind == ApiErrorKind.unauthorized) {
        await storage.clear();
        return null;
      }
      // Backend unreachable: trust the stored, unexpired session so the app
      // still opens; the next 401 forces a logout anyway.
      return Session(
        userId: stored.userId,
        email: stored.email,
        role: stored.role == 'DOCTOR' ? UserRole.doctor : UserRole.patient,
      );
    }
  }

  /// Called by the auth screens after a successful login/register.
  Future<void> signIn(AuthResponse auth) async {
    await ref.read(tokenStorageProvider).save(
          StoredSession(
            token: auth.token,
            userId: auth.userId,
            email: auth.email,
            role: auth.role == UserRole.doctor ? 'DOCTOR' : 'PATIENT',
            expiresAt: DateTime.now()
                .add(Duration(milliseconds: auth.expiresInMs)),
          ),
        );
    state = AsyncData(
      Session(userId: auth.userId, email: auth.email, role: auth.role),
    );
  }

  Future<void> logout() async {
    await ref.read(tokenStorageProvider).clear();
    state = const AsyncData(null);
  }
}
