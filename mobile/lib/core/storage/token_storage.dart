import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// What survives an app restart: enough to show the UI immediately and to
/// re-validate the token against `GET /api/auth/me` on boot.
class StoredSession {
  const StoredSession({
    required this.token,
    required this.userId,
    required this.username,
    required this.email,
    required this.role,
    required this.expiresAt,
    this.refreshToken,
  });

  final String token;

  /// Rotating refresh credential; null for sessions created before refresh
  /// tokens existed (or when the backend didn't issue one).
  final String? refreshToken;
  final int userId;

  /// Stable account handle used as the display identity.
  final String username;

  /// Optional: phone-only / social accounts have no email.
  final String? email;

  /// Raw role string as issued by the backend (`PATIENT` / `DOCTOR`).
  final String role;

  /// Local instant after which the token is certainly rejected.
  final DateTime expiresAt;

  bool get isExpired => DateTime.now().isAfter(expiresAt);
}

/// Persists the session in the platform keystore and mirrors the tokens in
/// memory so the dio request/retry interceptor can read them synchronously.
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _kToken = 'auth_token';
  static const _kRefreshToken = 'auth_refresh_token';
  static const _kUserId = 'auth_user_id';
  static const _kUsername = 'auth_username';
  static const _kEmail = 'auth_email';
  static const _kRole = 'auth_role';
  static const _kExpiresAt = 'auth_expires_at_epoch_ms';

  String? _cachedToken;
  String? _cachedRefreshToken;

  /// Last access token loaded or saved in this process; null when logged out.
  String? get cachedToken => _cachedToken;

  /// Last refresh token loaded or saved in this process; null when logged out.
  String? get cachedRefreshToken => _cachedRefreshToken;

  Future<StoredSession?> read() async {
    final token = await _storage.read(key: _kToken);
    final userId = int.tryParse(await _storage.read(key: _kUserId) ?? '');
    final username = await _storage.read(key: _kUsername);
    final role = await _storage.read(key: _kRole);
    final expiresAtMs =
        int.tryParse(await _storage.read(key: _kExpiresAt) ?? '');
    if (token == null ||
        userId == null ||
        username == null ||
        role == null ||
        expiresAtMs == null) {
      return null;
    }
    _cachedToken = token;
    _cachedRefreshToken = await _storage.read(key: _kRefreshToken);
    return StoredSession(
      token: token,
      refreshToken: _cachedRefreshToken,
      userId: userId,
      username: username,
      email: await _storage.read(key: _kEmail),
      role: role,
      expiresAt: DateTime.fromMillisecondsSinceEpoch(expiresAtMs),
    );
  }

  Future<void> save(StoredSession session) async {
    _cachedToken = session.token;
    _cachedRefreshToken = session.refreshToken;
    await _storage.write(key: _kToken, value: session.token);
    await _storage.write(key: _kUserId, value: session.userId.toString());
    await _storage.write(key: _kUsername, value: session.username);
    await _storage.write(key: _kRole, value: session.role);
    await _storage.write(
      key: _kExpiresAt,
      value: session.expiresAt.millisecondsSinceEpoch.toString(),
    );
    await _writeOrDelete(_kRefreshToken, session.refreshToken);
    await _writeOrDelete(_kEmail, session.email);
  }

  /// Replace just the tokens after a successful refresh, leaving identity intact.
  Future<void> updateTokens({
    required String token,
    required String? refreshToken,
    required DateTime expiresAt,
  }) async {
    _cachedToken = token;
    _cachedRefreshToken = refreshToken;
    await _storage.write(key: _kToken, value: token);
    await _storage.write(
      key: _kExpiresAt,
      value: expiresAt.millisecondsSinceEpoch.toString(),
    );
    await _writeOrDelete(_kRefreshToken, refreshToken);
  }

  Future<void> clear() async {
    _cachedToken = null;
    _cachedRefreshToken = null;
    for (final key in [
      _kToken,
      _kRefreshToken,
      _kUserId,
      _kUsername,
      _kEmail,
      _kRole,
      _kExpiresAt,
    ]) {
      await _storage.delete(key: key);
    }
  }

  Future<void> _writeOrDelete(String key, String? value) async {
    if (value == null) {
      await _storage.delete(key: key);
    } else {
      await _storage.write(key: key, value: value);
    }
  }
}
