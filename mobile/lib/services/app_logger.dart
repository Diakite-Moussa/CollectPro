import 'package:flutter/foundation.dart';

/// Logger minimal : centralise les traces d'erreurs "best-effort"
/// (celles qu'on avale volontairement pour ne pas bloquer l'app,
/// mais qu'on veut quand même voir en debug/tests terrain).
class AppLogger {
  static void warn(String context, Object error, [StackTrace? stack]) {
    if (kDebugMode) {
      debugPrint('[WARN] $context: $error');
      if (stack != null) debugPrint(stack.toString());
    }
    // TODO plus tard : envoyer vers un service de crash reporting
    // (Sentry, Crashlytics...) si le projet en ajoute un.
  }
}