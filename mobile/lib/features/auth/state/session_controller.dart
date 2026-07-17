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
    required this.username,
    required this.email,
    required this.role,
    this.emailVerified = false,
    this.phoneVerified = false,
  });

  final int userId;

  /// Stable display identity; always present.
  final String username;

  /// Optional: phone-only / social accounts have no email.
  final String? email;
  final UserRole role;

  /// Contact-verification state, sourced from `/me`. Defaults to false when the
  /// session is restored offline without a fresh `/me` round-trip.
  final bool emailVerified;
  final bool phoneVerified;
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
    if (stored.isExpired && stored.refreshToken == null) {
      // No way to renew an expired session without a refresh token.
      await storage.clear();
      return null;
    }
    try {
      final me = await ref.read(authRepositoryProvider).me();
      return Session(
        userId: me.userId,
        username: me.username,
        email: me.email,
        role: me.role,
        emailVerified: me.emailVerified,
        phoneVerified: me.phoneVerified,
      );
    } on ApiException catch (e) {
      if (e.kind == ApiErrorKind.unauthorized) {
        // A 401 here means refresh already failed in the interceptor.
        await storage.clear();
        return null;
      }
      // Backend unreachable: trust the stored session so the app still opens;
      // the next 401 triggers a refresh, and a failed refresh forces logout.
      return Session(
        userId: stored.userId,
        username: stored.username,
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
            refreshToken: auth.refreshToken,
            userId: auth.userId,
            username: auth.username,
            email: auth.email,
            role: auth.role == UserRole.doctor ? 'DOCTOR' : 'PATIENT',
            expiresAt: DateTime.now()
                .add(Duration(milliseconds: auth.expiresInMs)),
          ),
        );
    state = AsyncData(
      Session(
        userId: auth.userId,
        username: auth.username,
        email: auth.email,
        role: auth.role,
      ),
    );
  }

  Future<void> logout() async {
    final refreshToken = ref.read(tokenStorageProvider).cachedRefreshToken;
    // Best-effort server-side revocation; ignore failures (we log out locally regardless).
    if (refreshToken != null) {
      try {
        await ref.read(authRepositoryProvider).logout(refreshToken);
      } on ApiException {
        // ignore
      }
    }
    await _clearLocally();
  }

  /// Revoke every session for this account server-side, then log out locally.
  Future<void> logoutAll() async {
    try {
      await ref.read(authRepositoryProvider).logoutAll();
    } on ApiException {
      // ignore — we still clear locally so the device is signed out
    }
    await _clearLocally();
  }

  Future<void> _clearLocally() async {
    await ref.read(tokenStorageProvider).clear();
    state = const AsyncData(null);
  }
}
