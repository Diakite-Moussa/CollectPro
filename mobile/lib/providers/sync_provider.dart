import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/connectivity_service.dart';
import '../services/sync_service.dart';
import 'auth_provider.dart';
import 'database_provider.dart';

class SyncState {
  final bool isSyncing;
  final DateTime? lastSyncTime;
  final SyncResult? lastResult;
  final String? errorMessage;

  SyncState({
    this.isSyncing = false,
    this.lastSyncTime,
    this.lastResult,
    this.errorMessage,
  });

  SyncState copyWith({
    bool? isSyncing,
    DateTime? lastSyncTime,
    SyncResult? lastResult,
    String? errorMessage,
  }) {
    return SyncState(
      isSyncing: isSyncing ?? this.isSyncing,
      lastSyncTime: lastSyncTime ?? this.lastSyncTime,
      lastResult: lastResult ?? this.lastResult,
      errorMessage: errorMessage,
    );
  }
}

final connectivityServiceProvider = Provider<ConnectivityService>((ref) {
  return ConnectivityService();
});

final isOnlineStreamProvider = StreamProvider<bool>((ref) {
  final service = ref.watch(connectivityServiceProvider);
  return service.onConnectivityChanged;
});

final syncServiceProvider = Provider<SyncService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  final collecteDao = ref.watch(collecteDaoProvider);
  return SyncService(apiClient: apiClient, collecteDao: collecteDao);
});

class SyncController extends StateNotifier<SyncState> {
  final SyncService _syncService;
  final ConnectivityService _connectivityService;
  StreamSubscription<bool>? _connectivitySubscription;

  SyncController(this._syncService, this._connectivityService) : super(SyncState()) {
    _initNetworkListener();
  }

  void _initNetworkListener() {
    _connectivitySubscription = _connectivityService.onConnectivityChanged.listen((isOnline) {
      if (isOnline) {
        // En cas de retour de la connexion Internet, déclencher la synchro automatique
        syncNow();
      }
    });
  }

  Future<SyncResult?> syncNow() async {
    if (state.isSyncing) return null;

    final isConnected = await _connectivityService.isConnected();
    if (!isConnected) {
      state = state.copyWith(
        isSyncing: false,
        errorMessage: 'Impossible de synchroniser : aucun réseau disponible.',
      );
      return null;
    }

    state = state.copyWith(isSyncing: true, errorMessage: null);

    try {
      final result = await _syncService.syncPendingCollectes();
      state = state.copyWith(
        isSyncing: false,
        lastSyncTime: DateTime.now(),
        lastResult: result,
      );
      return result;
    } catch (e) {
      state = state.copyWith(
        isSyncing: false,
        errorMessage: e.toString(),
      );
      return null;
    }
  }

  @override
  void dispose() {
    _connectivitySubscription?.cancel();
    super.dispose();
  }
}

final syncControllerProvider = StateNotifierProvider<SyncController, SyncState>((ref) {
  final syncService = ref.watch(syncServiceProvider);
  final connectivityService = ref.watch(connectivityServiceProvider);
  return SyncController(syncService, connectivityService);
});
