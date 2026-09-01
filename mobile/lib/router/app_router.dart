import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/auth_provider.dart';
import '../screens/activation_screen.dart';
import '../screens/collecte_detail_screen.dart';
import '../screens/collecte_form_screen.dart';
import '../screens/edit_rejected_collecte_screen.dart';
import '../screens/form_list_screen.dart';
import '../screens/login_screen.dart';
import '../screens/main_navigation_screen.dart';
import '../screens/reset_password_screen.dart';
import '../screens/splash_screen.dart';
import '../screens/mission_list_screen.dart';

/// Notifie GoRouter quand l'état d'auth change, SANS recréer le router
/// (contrairement à un Provider qui ferait un ref.watch direct).
class _RouterRefreshNotifier extends ChangeNotifier {
  _RouterRefreshNotifier(Ref ref) {
    ref.listen(authControllerProvider, (prev, next) => notifyListeners());
  }
}

final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = _RouterRefreshNotifier(ref);

  return GoRouter(
    initialLocation: '/splash',
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final authState = ref.read(authControllerProvider);
      final isAuthenticated = authState.status == AuthStatus.authenticated;
      final isUnknown = authState.status == AuthStatus.unknown;
      final goingToSplash = state.matchedLocation == '/splash';
      final goingToActivation = state.matchedLocation.startsWith('/activate');
      final goingToResetPassword = state.matchedLocation.startsWith('/reset-password');

      // /activate et /reset-password doivent toujours être accessibles, quel que soit l'état d'auth
      if (goingToActivation || goingToResetPassword) return null;

      if (isUnknown && !goingToSplash) return '/splash';
      if (!isUnknown && goingToSplash) {
        return isAuthenticated ? '/home' : '/login';
      }
      if (!isAuthenticated && state.matchedLocation == '/home') return '/login';
      if (isAuthenticated && state.matchedLocation == '/login') return '/home';

      return null;
    },
    routes: [
      GoRoute(path: '/splash', builder: (context, state) => const SplashScreen()),
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(
        path: '/activate',
        builder: (context, state) {
          final token = state.uri.queryParameters['token'] ?? '';
          return ActivationScreen(token: token);
        },
      ),
      GoRoute(
        path: '/reset-password',
        builder: (context, state) {
          final token = state.uri.queryParameters['token'] ?? '';
          return ResetPasswordScreen(token: token);
        },
      ),
      GoRoute(path: '/home', builder: (context, state) => const MainNavigationScreen()),
      GoRoute(path: '/forms', builder: (context, state) => const FormListScreen()),
      GoRoute(
        path: '/forms/edit-rejected/:id',
        builder: (context, state) {
          final idStr = state.pathParameters['id'] ?? '0';
          final id = int.tryParse(idStr) ?? 0;
          return EditRejectedCollecteScreen(localId: id);
        },
      ),
      GoRoute(
        path: '/collecte-detail/:id',
        builder: (context, state) {
          final idStr = state.pathParameters['id'] ?? '0';
          final id = int.tryParse(idStr) ?? 0;
          return CollecteDetailScreen(localId: id);
        },
      ),
      GoRoute(
        path: '/collecte-form',
        builder: (context, state) {
          final formVersionId = int.tryParse(state.uri.queryParameters['formVersionId'] ?? '') ?? 0;
          final formTitle = state.uri.queryParameters['title'] ?? 'Collecte';
          final schemaJson = state.uri.queryParameters['schema'] ?? '{"fields":[]}';
          return CollecteFormScreen(
            formVersionId: formVersionId,
            formTitle: formTitle,
            schemaJson: schemaJson,
          );
        },
      ),

      GoRoute(path: '/missions', builder: (context, state) => const MissionListScreen()),
    ],
  );
});
