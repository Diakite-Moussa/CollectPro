import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

import 'daos/collecte_dao.dart';
import 'tables/collecte_table.dart';

part 'database.g.dart';

@DriftDatabase(tables: [CollectesTable], daos: [CollecteDao])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(driftDatabase(name: 'collectpro'));
  AppDatabase.executor(super.e);

  @override
  int get schemaVersion => 3;

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
        },
      );
}
