import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/user.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/token_storage.dart';

final tokenStorageProvider = Provider((ref) => TokenStorage());

final apiClientProvider = Provider((ref) => ApiClient(ref.watch(tokenStorageProvider)));

final authServiceProvider = Provider((ref) => AuthService(
  ref.watch(apiClientProvider),
  ref.watch(tokenStorageProvider),
));

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  final AuthStatus status;
  final AppUser? user;
  final String? errorMessage;

  AuthState({required this.status, this.user, this.errorMessage});

  AuthState copyWith({AuthStatus? status, AppUser? user, String? errorMessage}) {
    return AuthState(
      status: status ?? this.status,
      user: user ?? this.user,
      errorMessage: errorMessage,
    );
  }
}

class AuthController extends StateNotifier<AuthState> {
  final AuthService _authService;

  AuthController(this._authService) : super(AuthState(status: AuthStatus.unknown)) {
    _checkInitialSession();
  }

  void bindApiClient(ApiClient apiClient) {
    apiClient.onSessionExpired = () {
      logout();
    };
  }

  Future<void> _checkInitialSession() async {
    final hasSession = await _authService.hasValidSession();
    state = state.copyWith(
      status: hasSession ? AuthStatus.authenticated : AuthStatus.unauthenticated,
    );
  }

  Future<bool> login(String email, String password) async {
    try {
      final auth = await _authService.login(email, password);
      state = AuthState(status: AuthStatus.authenticated, user: auth.user);
      return true;
    } on AuthException catch (e) {
      state = state.copyWith(status: AuthStatus.unauthenticated, errorMessage: e.message);
      return false;
    }
  }

  Future<void> logout() async {
    await _authService.logout();
    state = AuthState(status: AuthStatus.unauthenticated);
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  final controller = AuthController(ref.watch(authServiceProvider));
  controller.bindApiClient(apiClient);
  return controller;
});