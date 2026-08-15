import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';

class ConnectivityService {
  final Connectivity _connectivity = Connectivity();

  /// Vérifie si l'appareil est connecté à Internet (Wi-Fi, Mobile ou Ethernet).
  Future<bool> isConnected() async {
    final results = await _connectivity.checkConnectivity();
    return _hasInternetConnection(results);
  }

  /// Flux réactif notifiant les changements d'état de la connexion.
  Stream<bool> get onConnectivityChanged {
    return _connectivity.onConnectivityChanged.map(_hasInternetConnection);
  }

  bool _hasInternetConnection(List<ConnectivityResult> results) {
    return results.any((result) =>
        result == ConnectivityResult.mobile ||
        result == ConnectivityResult.wifi ||
        result == ConnectivityResult.ethernet);
  }
}
