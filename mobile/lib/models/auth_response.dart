import 'user.dart';

class AuthResponse {
  final String accessToken;
  final String refreshToken;
  final int expiresIn;
  final String tokenType;
  final AppUser user;

  AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
    required this.tokenType,
    required this.user,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      expiresIn: json['expiresIn'] as int,
      tokenType: json['tokenType'] as String,
      user: AppUser.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}