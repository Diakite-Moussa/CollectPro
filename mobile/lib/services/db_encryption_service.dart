import 'dart:math';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class DbEncryptionService {
  static const _keyStorageKey = 'db_encryption_key';
  static const _secureStorage = FlutterSecureStorage();

  /// Récupère la clé de chiffrement existante, ou en génère une nouvelle
  /// (256 bits aléatoires) au tout premier lancement de l'application.
  static Future<String> getOrCreateKey() async {
    final existing = await _secureStorage.read(key: _keyStorageKey);
    if (existing != null && existing.isNotEmpty) {
      return existing;
    }
    final newKey = _generateSecureKey();
    await _secureStorage.write(key: _keyStorageKey, value: newKey);
    return newKey;
  }

  static String _generateSecureKey() {
    final random = Random.secure();
    final bytes = List<int>.generate(32, (_) => random.nextInt(256));
    return bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }
}
