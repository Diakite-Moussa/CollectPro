// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'collecte_dao.dart';

// ignore_for_file: type=lint
mixin _$CollecteDaoMixin on DatabaseAccessor<AppDatabase> {
  $CollectesTableTable get collectesTable => attachedDatabase.collectesTable;
  CollecteDaoManager get managers => CollecteDaoManager(this);
}

class CollecteDaoManager {
  final _$CollecteDaoMixin _db;
  CollecteDaoManager(this._db);
  $$CollectesTableTableTableManager get collectesTable =>
      $$CollectesTableTableTableManager(
        _db.attachedDatabase,
        _db.collectesTable,
      );
}
