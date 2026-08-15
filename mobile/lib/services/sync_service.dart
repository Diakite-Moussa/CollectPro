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

  SyncService({
    required ApiClient apiClient,
    required CollecteDao collecteDao,
  })  : _apiClient = apiClient,
        _collecteDao = collecteDao;

  /// Synchronise les collectes PENDING_SYNC via multipart (fichiers séparés du JSON).
  Future<SyncResult> syncPendingCollectes() async {
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

        Map<String, dynamic> dataPayload = {};
        try {
          dataPayload = jsonDecode(collecte.dataJson) as Map<String, dynamic>;
        } catch (_) {
          dataPayload = {'raw': collecte.dataJson};
        }

        final createRequest = {
          'formVersionId': collecte.formVersionId,
          'dataJson': jsonEncode(dataPayload),
          'latitude': collecte.latitude,
          'longitude': collecte.longitude,
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

        final response = await _apiClient.dio.post(
          '/collectes/upload',
          data: formData,
        );

        final responseData = response.data;
        final serverId = (responseData is Map && responseData.containsKey('id'))
            ? responseData['id'] as int
            : 0;

        await _deleteLocalMediaFiles(collecte);
        await _collecteDao.markAsSynced(collecte.id, serverId);
        syncedCount++;

        await _reportSyncAttempt(
          localReference: collecte.id.toString(),
          collecteId: serverId,
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
        if (collecteId != null) 'collecteId': collecteId,
        'result': result,
        if (errorMessage != null) 'errorMessage': errorMessage,
      });
    } catch (_) {
      // Silencieux : le sync-log est un journal secondaire, pas critique.
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
    } catch (_) {}
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
    } catch (_) {}
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
