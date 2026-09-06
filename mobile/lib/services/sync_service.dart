import 'app_logger.dart';
import 'dart:convert';
import 'dart:io';
import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';
import '../data/local/daos/collecte_dao.dart';
import 'api_client.dart';

class SyncResult {
  final int totalCount;
  final int syncedCount;
  final int failedCount;
  final List<String> errors;

  SyncResult({
    required this.totalCount,
    required this.syncedCount,
    required this.failedCount,
    required this.errors,
  });
}

class SyncService {
  final ApiClient _apiClient;
  final CollecteDao _collecteDao;

  // Constantes de retry & backoff
  static const int defaultMaxRetries = 3;
  static const Duration defaultInitialDelay = Duration(seconds: 1);
  static const double defaultBackoffMultiplier = 2.0;
  static const Duration defaultMaxDelay = Duration(seconds: 8);

  SyncService({
    required ApiClient apiClient,
    required CollecteDao collecteDao,
  })  : _apiClient = apiClient,
        _collecteDao = collecteDao;

  /// Synchronise les collectes PENDING_SYNC via multipart (fichiers séparés du JSON)
  /// avec gestion de retry et backoff exponentiel sur les erreurs transitoires.
  Future<SyncResult> syncPendingCollectes({
    int maxRetries = defaultMaxRetries,
    Duration initialDelay = defaultInitialDelay,
    double backoffMultiplier = defaultBackoffMultiplier,
    Duration maxDelay = defaultMaxDelay,
  }) async {
    final pendingList = await _collecteDao.getPendingSync();
    if (pendingList.isEmpty) {
      return SyncResult(
        totalCount: 0,
        syncedCount: 0,
        failedCount: 0,
        errors: [],
      );
    }

    int syncedCount = 0;
    int failedCount = 0;
    final List<String> errors = [];

    for (final collecte in pendingList) {
      try {
        await _collecteDao.markAsSyncing(collecte.id);

        final response = await _postCollecteWithRetry(
          collecte,
          maxRetries: maxRetries,
          initialDelay: initialDelay,
          backoffMultiplier: backoffMultiplier,
          maxDelay: maxDelay,
        );

        final responseData = response.data;
        final serverId = (responseData is Map && responseData.containsKey('id'))
            ? responseData['id'] as int
            : 0;

        await _deleteLocalMediaFiles(collecte);
        await _collecteDao.markAsSynced(collecte.id, serverId);
        syncedCount++;

        // Fix #1 : on n'envoie collecteId que si le serveur a retourné un ID valide (> 0).
        await _reportSyncAttempt(
          localReference: collecte.id.toString(),
          collecteId: serverId > 0 ? serverId : null,
          result: 'SUCCESS',
        );
      } on DioException catch (e) {
        failedCount++;
        final errorMsg = _extractErrorMessage(e);
        errors.add('Collecte #${collecte.id}: $errorMsg');
        await _collecteDao.markAsSyncFailed(collecte.id, errorMsg);

        final isDuplicate = e.response?.statusCode == 409;
        await _reportSyncAttempt(
          localReference: collecte.id.toString(),
          result: isDuplicate ? 'DUPLICATE' : 'ERROR',
          errorMessage: errorMsg,
        );

        // Si rupture de réseau totale confirmée, arrêt anticipé pour économiser la batterie
        if (e.type == DioExceptionType.connectionError || e.type == DioExceptionType.connectionTimeout) {
          AppLogger.warn('SyncService', 'Arrêt de la file de synchronisation suite à une perte de connexion réseau', e.stackTrace);
          break;
        }
      } catch (e) {
        failedCount++;
        final errorMsg = e.toString();
        errors.add('Collecte #${collecte.id}: $errorMsg');
        await _collecteDao.markAsSyncFailed(collecte.id, errorMsg);

        await _reportSyncAttempt(
          localReference: collecte.id.toString(),
          result: 'ERROR',
          errorMessage: errorMsg,
        );
      }
    }

    return SyncResult(
      totalCount: pendingList.length,
      syncedCount: syncedCount,
      failedCount: failedCount,
      errors: errors,
    );
  }

  /// Exécute l'envoi d'une collecte avec réessais et backoff exponentiel.
  Future<Response> _postCollecteWithRetry(
    dynamic collecte, {
    required int maxRetries,
    required Duration initialDelay,
    required double backoffMultiplier,
    required Duration maxDelay,
  }) async {
    int attempt = 0;
    Duration currentDelay = initialDelay;

    while (true) {
      attempt++;
      try {
        // Construction fraîche du FormData à chaque tentative (streams non épuisés)
        final formData = await _buildFormData(collecte);
        return await _apiClient.dio.post(
          '/collectes/upload',
          data: formData,
        );
      } catch (e, st) {
        final isRetryable = _isRetryableError(e);

        if (attempt >= maxRetries || !isRetryable) {
          AppLogger.warn(
            'SyncService: Échec de transmission pour collecte #${collecte.id} après $attempt tentative(s)',
            e,
            st,
          );
          rethrow;
        }

        AppLogger.warn(
          'SyncService: Échec transitoire pour collecte #${collecte.id} (tentative $attempt/$maxRetries). Prochain essai dans ${currentDelay.inMilliseconds}ms',
          e,
          st,
        );

        await Future.delayed(currentDelay);
        final nextMs = (currentDelay.inMilliseconds * backoffMultiplier).round();
        currentDelay = Duration(milliseconds: nextMs.clamp(0, maxDelay.inMilliseconds));
      }
    }
  }

  /// Détermine si une erreur est transitoire et justifie un réessai.
  bool _isRetryableError(dynamic error) {
    if (error is DioException) {
      switch (error.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.sendTimeout:
        case DioExceptionType.receiveTimeout:
        case DioExceptionType.connectionError:
          return true;
        case DioExceptionType.badResponse:
          final status = error.response?.statusCode;
          // Erreurs serveur 5xx ou 429 Too Many Requests
          return status != null && (status >= 500 || status == 429);
        default:
          return error.error is SocketException;
      }
    }
    return error is SocketException;
  }

  /// Construit le FormData multipart pour la transmission d'une collecte.
  Future<FormData> _buildFormData(dynamic collecte) async {
    Map<String, dynamic> dataPayload = {};
    try {
      dataPayload = jsonDecode(collecte.dataJson) as Map<String, dynamic>;
    } catch (e, st) {
      AppLogger.warn(
        'SyncService._buildFormData (parsing dataJson collecte #${collecte.id})',
        e,
        st,
      );
      dataPayload = {'raw': collecte.dataJson};
    }

    final createRequest = {
      'formVersionId': collecte.formVersionId,
      'dataJson': jsonEncode(dataPayload),
      'latitude': collecte.latitude,
      'longitude': collecte.longitude,
      'missionId': ?collecte.missionId,
    };

    final formData = FormData.fromMap({
      'data': MultipartFile.fromString(
        jsonEncode(createRequest),
        contentType: MediaType('application', 'json'),
        filename: 'data.json',
      ),
    });

    await _appendFiles(formData, collecte.photoPaths);
    await _appendFiles(formData, collecte.documentPaths);

    return formData;
  }

  /// Envoie un log de tentative de synchronisation au serveur. Best-effort :
  /// un échec de ce log ne doit jamais faire planter le flux de sync principal.
  Future<void> _reportSyncAttempt({
    required String localReference,
    int? collecteId,
    required String result,
    String? errorMessage,
  }) async {
    try {
      await _apiClient.dio.post('/sync-logs', data: {
        'localReference': localReference,
        'collecteId': ?collecteId,
        'result': result,
        'errorMessage': ?errorMessage,
      });
    } catch (e, st) {
      AppLogger.warn('SyncService._reportSyncAttempt', e, st);
    }
  }

  Future<void> _appendFiles(FormData formData, String? pathsJson) async {
    if (pathsJson == null || pathsJson.isEmpty) return;
    try {
      final paths = jsonDecode(pathsJson) as List<dynamic>;
      for (final path in paths) {
        final file = File(path.toString());
        if (await file.exists()) {
          formData.files.add(MapEntry(
            'files',
            await MultipartFile.fromFile(path.toString()),
          ));
        }
      }
    } catch (e, st) {
      AppLogger.warn(
        'SyncService._appendFiles (collecte photos/docs)',
        e,
        st,
      );
    }
  }

  Future<void> _deleteLocalMediaFiles(dynamic collecte) async {
    await _deletePathsFromJson(collecte.photoPaths);
    await _deletePathsFromJson(collecte.documentPaths);
  }

  Future<void> _deletePathsFromJson(String? pathsJson) async {
    if (pathsJson == null || pathsJson.isEmpty) return;
    try {
      final paths = jsonDecode(pathsJson) as List<dynamic>;
      for (final path in paths) {
        final file = File(path.toString());
        if (await file.exists()) {
          await file.delete();
        }
      }
    } catch (e, st) {
      AppLogger.warn('SyncService._deletePathsFromJson', e, st);
    }
  }

  /// Récupère l'état de validation de toutes les collectes de l'agent
  /// (GET /collectes/mine) et met à jour les lignes locales correspondantes.
  Future<void> pullValidationStatuses() async {
    try {
      final response = await _apiClient.dio.get('/collectes/mine');
      final data = response.data as List;

      for (final item in data) {
        final json = item as Map<String, dynamic>;
        final serverId = json['id'] as int?;
        if (serverId == null) continue;

        final status = json['status'] as String?;
        if (status == null) continue;

        final validatedBy = json['validatedBy'] as Map<String, dynamic>?;
        final validatedByName = validatedBy != null
            ? '${validatedBy['firstName']} ${validatedBy['lastName']}'
            : null;

        await _collecteDao.updateValidationStatus(
          serverId: serverId,
          serverStatus: status,
          validationComment: json['validationComment'] as String?,
          validatedByName: validatedByName,
          validatedAt: json['validatedAt'] != null
              ? DateTime.parse(json['validatedAt'] as String)
              : null,
        );
      }
    } catch (e, st) {
      AppLogger.warn('SyncService.pullValidationStatuses', e, st);
    }
  }

  /// Soumet les modifications d'une collecte rejetée au serveur.
  Future<void> resubmitCollecte({
    required int serverId,
    required String dataJson,
    double? latitude,
    double? longitude,
  }) async {
    await _apiClient.dio.put('/collectes/$serverId/resubmit', data: {
      'dataJson': dataJson,
      'latitude': ?latitude,
      'longitude': ?longitude,
    });
  }

  String _extractErrorMessage(DioException e) {
    if (e.response != null && e.response?.data != null) {
      final data = e.response?.data;
      if (data is Map && data.containsKey('message')) {
        return data['message'].toString();
      }
    }
    return e.message ?? 'Serveur non joignable (hors-ligne)';
  }
}