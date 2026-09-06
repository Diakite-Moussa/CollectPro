import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqlcipher_flutter_libs/sqlcipher_flutter_libs.dart';
import 'package:sqlite3/open.dart';

import 'daos/collecte_dao.dart';
import 'tables/collecte_table.dart';
import '../../services/db_encryption_service.dart';

part 'database.g.dart';

// NOTE : on n'utilise volontairement pas `drift_flutter` ici. Ce package
// dépend en interne de `sqlite3_flutter_libs`, qui entre en conflit avec
// `sqlcipher_flutter_libs` (mêmes classes Android générées deux fois =
// crash au build : "Sqlite3FlutterLibsPlugin is defined multiple times").
// Voir https://github.com/simolus3/drift/issues/3702 (non résolu à ce jour).
// On ouvre donc la connexion manuellement avec NativeDatabase.

@DriftDatabase(tables: [CollectesTable], daos: [CollecteDao])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openEncryptedConnection());
  AppDatabase.executor(super.e);

  static QueryExecutor _openEncryptedConnection() {
    return LazyDatabase(() async {
      if (Platform.isAndroid) {
        // Indique à sqlite3 d'utiliser la bibliothèque SQLCipher plutôt que
        // la SQLite standard. Indispensable : sans ça, l'ouverture du
        // fichier chiffré échoue silencieusement ou plante.
        open.overrideFor(OperatingSystem.android, openCipherOnAndroid);
      }

      final dbFolder = await getApplicationDocumentsDirectory();
      final file = File(p.join(dbFolder.path, 'collectpro.sqlite'));
      final key = await DbEncryptionService.getOrCreateKey();

      return NativeDatabase.createInBackground(
        file,
        isolateSetup: () async {
          // NativeDatabase.createInBackground tourne dans un isolate séparé :
          // l'override doit être refait là aussi, sinon cet isolate utilise
          // la SQLite standard.
          if (Platform.isAndroid) {
            open.overrideFor(OperatingSystem.android, openCipherOnAndroid);
          }
        },
        setup: (rawDb) {
          final escapedKey = key.replaceAll("'", "''");
          // Doit être la toute première instruction exécutée sur la connexion
          rawDb.execute("PRAGMA key = '$escapedKey';");
          // Requis avec les binaires SQLCipher officiels (désactive une
          // ancienne fonctionnalité SQLite non standard).
          rawDb.config.doubleQuotedStringLiterals = false;
        },
      );
    });
  }

  @override
  int get schemaVersion => 5;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) => m.createAll(),
        onUpgrade: (m, from, to) async {
          if (from < 2) {
            // v1 -> v2 : ajout du rattachement agent + pièces jointes
            await m.addColumn(collectesTable, collectesTable.agentId);
            await m.addColumn(collectesTable, collectesTable.photoPaths);
            await m.addColumn(collectesTable, collectesTable.documentPaths);
          }
          if (from < 3) {
            // v2 -> v3 : ajout des détails d'erreur de synchro (Sprint 5)
            await m.addColumn(collectesTable, collectesTable.syncErrorMessage);
          }
          if (from < 4) {
            // v3 -> v4 : statut de validation renvoyé par le superviseur
            await m.addColumn(collectesTable, collectesTable.serverStatus);
            await m.addColumn(collectesTable, collectesTable.validationComment);
            await m.addColumn(collectesTable, collectesTable.validatedByName);
            await m.addColumn(collectesTable, collectesTable.validatedAt);
          }
          if (from < 5) {
            // v4 -> v5 : rattachement optionnel à une mission (Sprint 2 V1.1)
            await m.addColumn(collectesTable, collectesTable.missionId);
          }
        },
      );
}
