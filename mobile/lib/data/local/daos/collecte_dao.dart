import 'dart:convert';

import 'package:drift/drift.dart';

import '../database.dart';
import '../tables/collecte_table.dart';

part 'collecte_dao.g.dart';

@DriftAccessor(tables: [CollectesTable])
class CollecteDao extends DatabaseAccessor<AppDatabase> with _$CollecteDaoMixin {
  CollecteDao(super.db);

  /// Insère une nouvelle collecte en local.
  /// [readyForSync] : true dès que l'agent a soumis le formulaire complet
  /// (statut PENDING_SYNC direct) ; false pour un brouillon (statut DRAFT).
  Future<int> insertCollecte({
    required int formVersionId,
    required String dataJson,
    int? agentId,
    int? missionId,  // AJOUTÉ : identifiant de la mission
    double? latitude,
    double? longitude,
    List<String>? photoPaths,
    List<String>? documentPaths,
    bool readyForSync = false,
  }) {
    return into(collectesTable).insert(
      CollectesTableCompanion.insert(
        formVersionId: formVersionId,
        dataJson: dataJson,
        agentId: Value(agentId),
        missionId: Value(missionId),  // AJOUTÉ : liaison avec la mission
        latitude: Value(latitude),
        longitude: Value(longitude),
        photoPaths: Value(photoPaths == null ? null : _encodeList(photoPaths)),
        documentPaths: Value(documentPaths == null ? null : _encodeList(documentPaths)),
        localStatus: Value(readyForSync ? 'PENDING_SYNC' : 'DRAFT'),
      ),
    );
  }

  /// Met à jour les données d'une collecte locale existante (brouillon ou réédition après échec).
  Future<void> updateCollecte({
    required int localId,
    required String dataJson,
    double? latitude,
    double? longitude,
    List<String>? photoPaths,
    List<String>? documentPaths,
    bool readyForSync = true,
  }) {
    return (update(collectesTable)..where((t) => t.id.equals(localId))).write(
      CollectesTableCompanion(
        dataJson: Value(dataJson),
        latitude: Value(latitude),
        longitude: Value(longitude),
        photoPaths: Value(photoPaths == null ? null : _encodeList(photoPaths)),
        documentPaths: Value(documentPaths == null ? null : _encodeList(documentPaths)),
        localStatus: Value(readyForSync ? 'PENDING_SYNC' : 'DRAFT'),
        syncErrorMessage: const Value(null),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Passe une collecte de DRAFT à PENDING_SYNC (prête à être envoyée).
  Future<void> markAsPendingSync(int localId) {
    return (update(collectesTable)..where((t) => t.id.equals(localId))).write(
      CollectesTableCompanion(
        localStatus: const Value('PENDING_SYNC'),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Toutes les collectes locales (pour un écran "mes collectes").
  Future<List<CollecteRow>> getAllCollectes() {
    return (select(collectesTable)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .get();
  }

  /// Stream réactif : l'UI se met à jour automatiquement à chaque changement.
  Stream<List<CollecteRow>> watchAllCollectes() {
    return (select(collectesTable)
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .watch();
  }

  /// Les collectes en attente de synchronisation (utile dès le Sprint 5).
  Future<List<CollecteRow>> getPendingSync() {
    return (select(collectesTable)
      ..where((t) => t.localStatus.equals('PENDING_SYNC')))
        .get();
  }

  /// Passe une collecte à l'état SYNCING pendant l'envoi vers l'API.
  Future<void> markAsSyncing(int localId) {
    return (update(collectesTable)..where((t) => t.id.equals(localId))).write(
      CollectesTableCompanion(
        localStatus: const Value('SYNCING'),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Marque la collecte comme SYNCHRONISÉE avec succès et stocke le serverId.
  Future<void> markAsSynced(int localId, int serverId) {
    return (update(collectesTable)..where((t) => t.id.equals(localId))).write(
      CollectesTableCompanion(
        serverId: Value(serverId),
        localStatus: const Value('SYNCED'),
        syncErrorMessage: const Value(null),
        photoPaths: const Value(null),
        documentPaths: const Value(null),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Remet une collecte en échec au statut PENDING_SYNC pour retenter l'envoi.
  Future<void> retryFailedCollecte(int localId) {
    return (update(collectesTable)..where((t) => t.id.equals(localId))).write(
      CollectesTableCompanion(
        localStatus: const Value('PENDING_SYNC'),
        syncErrorMessage: const Value(null),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Remet toutes les collectes en échec au statut PENDING_SYNC.
  Future<void> retryAllFailedCollectes() {
    return (update(collectesTable)..where((t) => t.localStatus.equals('SYNC_FAILED'))).write(
      CollectesTableCompanion(
        localStatus: const Value('PENDING_SYNC'),
        syncErrorMessage: const Value(null),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Marque l'échec de synchronisation avec le message d'erreur.
  Future<void> markAsSyncFailed(int localId, String errorMessage) {
    return (update(collectesTable)..where((t) => t.id.equals(localId))).write(
      CollectesTableCompanion(
        localStatus: const Value('SYNC_FAILED'),
        syncErrorMessage: Value(errorMessage),
        updatedAt: Value(DateTime.now()),
      ),
    );
  }

  /// Met à jour le statut de validation renvoyé par le serveur, en retrouvant
  /// la ligne locale via son serverId.
  Future<void> updateValidationStatus({
    required int serverId,
    required String serverStatus,
    String? validationComment,
    String? validatedByName,
    DateTime? validatedAt,
  }) {
    return (update(collectesTable)..where((t) => t.serverId.equals(serverId))).write(
      CollectesTableCompanion(
        serverStatus: Value(serverStatus),
        validationComment: Value(validationComment),
        validatedByName: Value(validatedByName),
        validatedAt: Value(validatedAt),
      ),
    );
  }

  /// Récupère une collecte locale par son serverId (pour pré-remplir l'écran
  /// de correction après un rejet).
  Future<CollecteRow?> getByServerId(int serverId) {
    return (select(collectesTable)..where((t) => t.serverId.equals(serverId)))
        .getSingleOrNull();
  }

  /// Récupère une collecte locale par son identifiant local id.
  Future<CollecteRow?> getById(int localId) {
    return (select(collectesTable)..where((t) => t.id.equals(localId)))
        .getSingleOrNull();
  }

  /// Récupère toutes les collectes d'une mission spécifique.
  Future<List<CollecteRow>> getCollectesByMission(int missionId) {
    return (select(collectesTable)
      ..where((t) => t.missionId.equals(missionId))
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .get();
  }

  /// Stream réactif des collectes d'une mission spécifique.
  Stream<List<CollecteRow>> watchCollectesByMission(int missionId) {
    return (select(collectesTable)
      ..where((t) => t.missionId.equals(missionId))
      ..orderBy([(t) => OrderingTerm.desc(t.createdAt)]))
        .watch();
  }

  /// Supprime une collecte locale spécifique par son ID.
  Future<int> deleteCollecte(int localId) {
    return (delete(collectesTable)..where((t) => t.id.equals(localId))).go();
  }

  /// Efface toutes les collectes locales de la base SQLite de l'appareil.
  Future<int> clearAllCollectes() {
    return delete(collectesTable).go();
  }

  /// Supprime toutes les collectes d'une mission spécifique.
  Future<int> deleteCollectesByMission(int missionId) {
    return (delete(collectesTable)..where((t) => t.missionId.equals(missionId))).go();
  }

  /// Compte le nombre de collectes pour une mission donnée (COUNT SQL,
  /// sans charger les lignes en mémoire).
  Future<int> countCollectesByMission(int missionId) {
    final query = selectOnly(collectesTable)
      ..where(collectesTable.missionId.equals(missionId))
      ..addColumns([collectesTable.id.count()]);
    return query.map((row) => row.read(collectesTable.id.count()) ?? 0).getSingle();
  }

  String _encodeList(List<String> items) => jsonEncode(items);
}