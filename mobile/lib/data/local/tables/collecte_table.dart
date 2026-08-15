import 'package:drift/drift.dart';

@DataClassName('CollecteRow')
class CollectesTable extends Table {
  IntColumn get id => integer().autoIncrement()();

  IntColumn get serverId => integer().nullable()();

  IntColumn get formVersionId => integer()();

  /// Agent qui a saisi la collecte (User.id côté backend).
  /// Nullable pour compatibilité avec les lignes créées avant cette colonne.
  IntColumn get agentId => integer().nullable()();

  TextColumn get dataJson => text()();

  RealColumn get latitude => real().nullable()();

  RealColumn get longitude => real().nullable()();

  /// Chemins locaux des photos jointes, encodés en JSON (liste de String).
  /// Capture réelle non implémentée : nécessite le package image_picker.
  TextColumn get photoPaths => text().nullable()();

  /// Chemins locaux des documents joints, encodés en JSON (liste de String).
  TextColumn get documentPaths => text().nullable()();

  TextColumn get localStatus =>
      text().withDefault(const Constant('DRAFT'))();

  TextColumn get syncErrorMessage => text().nullable()();

  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();

  DateTimeColumn get updatedAt =>
      dateTime().withDefault(currentDateAndTime)();
}
