import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/local/database.dart';

/// Instance unique de la base Drift, partagée dans toute l'app.
final appDatabaseProvider = Provider<AppDatabase>((ref) {
  final db = AppDatabase();
  ref.onDispose(() => db.close());
  return db;
});

/// Accès direct au DAO des collectes, sans repasser par appDatabaseProvider
/// à chaque fois.
final collecteDaoProvider = Provider((ref) {
  return ref.watch(appDatabaseProvider).collecteDao;
});