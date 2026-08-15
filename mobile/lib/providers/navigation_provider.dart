import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Index de l'onglet actif dans MainNavigationScreen (0 = Accueil, 1 = Synchro, 2 = Profil).
final mainNavIndexProvider = StateProvider<int>((ref) => 0);
