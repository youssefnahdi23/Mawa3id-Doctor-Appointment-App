import 'package:json_annotation/json_annotation.dart';

part 'auth_models.g.dart';

enum UserRole {
  @JsonValue('PATIENT')
  patient,
  @JsonValue('DOCTOR')
  doctor,
}

@JsonSerializable(createToJson: false)
class AuthResponse {
  const AuthResponse({
    required this.token,
    required this.tokenType,
    required this.expiresInMs,
    required this.userId,
    required this.username,
    required this.email,
    required this.role,
    this.refreshToken,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) =>
      _$AuthResponseFromJson(json);

  final String token;
  final String tokenType;
  final int expiresInMs;

  /// Opaque, rotating credential used to obtain a fresh access token. Nullable so
  /// older backends that don't issue one still parse.
  final String? refreshToken;
  final int userId;

  /// Stable account handle; always present on the current backend.
  final String username;

  /// Optional as of the auth overhaul (phone-only / social accounts have none).
  final String? email;
  final UserRole role;
}

@JsonSerializable(createToJson: false)
class MeResponse {
  const MeResponse({
    required this.userId,
    required this.username,
    required this.email,
    required this.role,
    this.emailVerified = false,
    this.phoneVerified = false,
  });

  factory MeResponse.fromJson(Map<String, dynamic> json) =>
      _$MeResponseFromJson(json);

  final int userId;
  final String username;
  final String? email;
  final UserRole role;

  /// Contact-verification state; defaults to false so older backends parse.
  @JsonKey(defaultValue: false)
  final bool emailVerified;
  @JsonKey(defaultValue: false)
  final bool phoneVerified;
}
