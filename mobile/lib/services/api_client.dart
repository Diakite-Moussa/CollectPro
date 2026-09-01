import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'token_storage.dart';
import '../constante/environnement.dart';

String get kBaseUrl {
  const envUrl = String.fromEnvironment('API_URL');
  if (envUrl.isNotEmpty) {
    return envUrl;
  }
  if (kIsWeb) {
    return Environnement().webappUrl;
  }
  if (defaultTargetPlatform == TargetPlatform.android) {
    return 'http://10.0.2.2:8080';
  }
  return 'http://localhost:8080';
}

typedef SessionExpiredCallback = void Function();

class ApiClient {
  final Dio dio;
  final TokenStorage _tokenStorage;
  SessionExpiredCallback? onSessionExpired;

  ApiClient(this._tokenStorage)
      : dio = Dio(BaseOptions(
          baseUrl: kBaseUrl,
          connectTimeout: const Duration(seconds: 30),
          receiveTimeout: const Duration(seconds: 30),
          headers: {'X-Client-Platform': 'MOBILE'},
        )) {
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        if (!_isPublicRoute(options.path)) {
          final token = await _tokenStorage.getAccessToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
        }
        handler.next(options);
      },
      onError: (DioException error, handler) async {
        final path = error.requestOptions.path;
        if (error.response?.statusCode == 401 &&
            !_isPublicRoute(path) &&
            !path.contains('/refresh') &&
            error.requestOptions.extra['retried'] != true) {
          final refreshed = await refreshTokens();
          if (refreshed) {
            error.requestOptions.extra['retried'] = true;
            final token = await _tokenStorage.getAccessToken();
            error.requestOptions.headers['Authorization'] = 'Bearer $token';
            try {
              final response = await dio.fetch(error.requestOptions);
              return handler.resolve(response);
            } catch (_) {}
          } else {
            onSessionExpired?.call();
          }
        }
        handler.next(error);
      },
    ));
  }

  bool _isPublicRoute(String path) {
    return path.contains('/login') ||
        path.contains('/activate') ||
        path.contains('/refresh') ||
        path.contains('/organizations');
  }

  /// Tente de renouveler les tokens. Retourne true si succès.
  Future<bool> refreshTokens() async {
    final refreshToken = await _tokenStorage.getRefreshToken();
    if (refreshToken == null) return false;
    try {
      final response = await dio.post('/refresh', data: {'refreshToken': refreshToken});
      final newAccessToken = response.data['accessToken'] as String;
      final newRefreshToken = response.data['refreshToken'] as String? ?? refreshToken;
      await _tokenStorage.saveTokens(
        accessToken: newAccessToken,
        refreshToken: newRefreshToken,
      );
      return true;
    } catch (_) {
      await _tokenStorage.clear();
      return false;
    }
  }

  Future<Response<T>> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return dio.post<T>(path, data: data, queryParameters: queryParameters, options: options);
  }

  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) {
    return dio.get<T>(path, queryParameters: queryParameters, options: options);
  }
}
