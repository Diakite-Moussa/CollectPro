import 'package:dio/dio.dart';
import '../models/activation_token_status.dart';
import '../models/auth_response.dart';
import 'api_client.dart';
import 'token_storage.dart';

class AuthException implements Exception {
  final String message;
  AuthException(this.message);
}

class AuthService {
  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  AuthService(this._apiClient, this._tokenStorage);

  Future<AuthResponse> login(String email, String password) async {
    try {
      final response = await _apiClient.dio.post('/login', data: {
        'email': email,
        'password': password,
      });
      final auth = AuthResponse.fromJson(response.data as Map<String, dynamic>);
      await _tokenStorage.saveTokens(
        accessToken: auth.accessToken,
        refreshToken: auth.refreshToken,
      );
      return auth;
    } on DioException catch (e) {
      final message = e.response?.data?['message'] as String? ?? 'Erreur de connexion';
      throw AuthException(message);
    }
  }

  Future<void> activate(String token, String password) async {
    try {
      await _apiClient.dio.post('/activate', data: {
        'token': token,
        'password': password,
      });
    } on DioException catch (e) {
      final message = e.response?.data?['message'] as String? ?? 'Erreur d\'activation';
      throw AuthException(message);
    }
  }

  Future<void> forgotPassword(String email) async {
    try {
      await _apiClient.dio.post('/forgot-password', data: {
        'email': email,
      });
    } on DioException catch (e) {
      final message = e.response?.data?['message'] as String? ?? 'Erreur lors de la demande';
      throw AuthException(message);
    }
  }

  Future<void> resetPassword(String token, String newPassword) async {
    try {
      await _apiClient.dio.post('/reset-password', data: {
        'token': token,
        'newPassword': newPassword,
      });
    } on DioException catch (e) {
      final message = e.response?.data?['message'] as String? ?? 'Erreur lors de la réinitialisation';
      throw AuthException(message);
    }
  }

  Future<ActivationTokenStatus> checkActivationToken(String token) async {
    try {
      final response = await _apiClient.dio.get('/activate', queryParameters: {'token': token});
      return ActivationTokenStatus.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      final message = e.response?.data?['message'] as String? ??
          'Ce lien d\'activation est invalide ou a expiré';
      throw AuthException(message);
    }
  }

  Future<void> logout() => _tokenStorage.clear();

  /// Vérifie la session : access token présent, ou refresh token utilisable.
  Future<bool> hasValidSession() async {
    final accessToken = await _tokenStorage.getAccessToken();
    if (accessToken != null) return true;

    final refreshToken = await _tokenStorage.getRefreshToken();
    if (refreshToken == null) return false;

    return _apiClient.refreshTokens();
  }
}
