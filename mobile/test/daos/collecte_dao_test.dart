import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:collectpro_mobile/data/local/database.dart';

void main() {
  late AppDatabase database;

  setUp(() {
    database = AppDatabase.executor(NativeDatabase.memory());
  });

  tearDown(() async {
    await database.close();
  });

  group('CollecteDao Tests', () {
    test('insertCollecte and getPendingSync', () async {
      final dao = database.collecteDao;

      final id = await dao.insertCollecte(
        formVersionId: 1,
        dataJson: '{"key":"value"}',
        agentId: 10,
        latitude: 14.69,
        longitude: -17.44,
        readyForSync: true,
      );

      expect(id, greaterThan(0));

      final pending = await dao.getPendingSync();
      expect(pending.length, 1);
      expect(pending.first.id, id);
      expect(pending.first.localStatus, 'PENDING_SYNC');
    });

    test('markAsSyncing and markAsSynced transitions', () async {
      final dao = database.collecteDao;

      final id = await dao.insertCollecte(
        formVersionId: 1,
        dataJson: '{"test":true}',
        readyForSync: true,
      );

      await dao.markAsSyncing(id);
      var pending = await dao.getPendingSync();
      expect(pending.isEmpty, true);

      await dao.markAsSynced(id, 999);
      final all = await dao.getAllCollectes();
      final synced = all.firstWhere((c) => c.id == id);
      expect(synced.localStatus, 'SYNCED');
      expect(synced.serverId, 999);
    });

    test('markAsSyncFailed and retry', () async {
      final dao = database.collecteDao;

      final id = await dao.insertCollecte(
        formVersionId: 1,
        dataJson: '{"test":true}',
        readyForSync: true,
      );

      await dao.markAsSyncFailed(id, 'Network timeout');

      var all = await dao.getAllCollectes();
      var failed = all.firstWhere((c) => c.id == id);
      expect(failed.localStatus, 'SYNC_FAILED');
      expect(failed.syncErrorMessage, 'Network timeout');

      await dao.retryFailedCollecte(id);
      all = await dao.getAllCollectes();
      failed = all.firstWhere((c) => c.id == id);
      expect(failed.localStatus, 'PENDING_SYNC');
      expect(failed.syncErrorMessage, null);
    });
  });
}
