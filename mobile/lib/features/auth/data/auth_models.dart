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
    required this.email,
    required this.role,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) =>
      _$AuthResponseFromJson(json);

  final String token;
  final String tokenType;
  final int expiresInMs;
  final int userId;
  final String email;
  final UserRole role;
}

@JsonSerializable(createToJson: false)
class MeResponse {
  const MeResponse({
    required this.userId,
    required this.email,
    required this.role,
  });

  factory MeResponse.fromJson(Map<String, dynamic> json) =>
      _$MeResponseFromJson(json);

  final int userId;
  final String email;
  final UserRole role;
}
