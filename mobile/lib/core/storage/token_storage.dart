import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// What survives an app restart: enough to show the UI immediately and to
/// re-validate the token against `GET /api/auth/me` on boot.
class StoredSession {
  const StoredSession({
    required this.token,
    required this.userId,
    required this.email,
    required this.role,
    required this.expiresAt,
  });

  final String token;
  final int userId;
  final String email;

  /// Raw role string as issued by the backend (`PATIENT` / `DOCTOR`).
  final String role;

  /// Local instant after which the token is certainly rejected.
  final DateTime expiresAt;

  bool get isExpired => DateTime.now().isAfter(expiresAt);
}

/// Persists the session in the platform keystore and mirrors the token in
/// memory so the dio request interceptor can read it synchronously.
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _kToken = 'auth_token';
  static const _kUserId = 'auth_user_id';
  static const _kEmail = 'auth_email';
  static const _kRole = 'auth_role';
  static const _kExpiresAt = 'auth_expires_at_epoch_ms';

  String? _cachedToken;

  /// Last token loaded or saved in this process; null when logged out.
  String? get cachedToken => _cachedToken;

  Future<StoredSession?> read() async {
    final token = await _storage.read(key: _kToken);
    final userId = int.tryParse(await _storage.read(key: _kUserId) ?? '');
    final email = await _storage.read(key: _kEmail);
    final role = await _storage.read(key: _kRole);
    final expiresAtMs =
        int.tryParse(await _storage.read(key: _kExpiresAt) ?? '');
    if (token == null ||
        userId == null ||
        email == null ||
        role == null ||
        expiresAtMs == null) {
      return null;
    }
    _cachedToken = token;
    return StoredSession(
      token: token,
      userId: userId,
      email: email,
      role: role,
      expiresAt: DateTime.fromMillisecondsSinceEpoch(expiresAtMs),
    );
  }

  Future<void> save(StoredSession session) async {
    _cachedToken = session.token;
    await _storage.write(key: _kToken, value: session.token);
    await _storage.write(key: _kUserId, value: session.userId.toString());
    await _storage.write(key: _kEmail, value: session.email);
    await _storage.write(key: _kRole, value: session.role);
    await _storage.write(
      key: _kExpiresAt,
      value: session.expiresAt.millisecondsSinceEpoch.toString(),
    );
  }

  Future<void> clear() async {
    _cachedToken = null;
    for (final key in [_kToken, _kUserId, _kEmail, _kRole, _kExpiresAt]) {
      await _storage.delete(key: key);
    }
  }
}
