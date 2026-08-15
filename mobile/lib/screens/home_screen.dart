import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../providers/auth_provider.dart';
import '../providers/navigation_provider.dart';
import '../providers/sync_provider.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;
    final syncState = ref.watch(syncControllerProvider);
    final syncNotifier = ref.read(syncControllerProvider.notifier);
    final isOnlineAsync = ref.watch(isOnlineStreamProvider);
    final isOnline = isOnlineAsync.value ?? false;

    return Scaffold(
      appBar: AppBar(
        title: Text('Bonjour, ${user?.firstName ?? ''}'),
        actions: [
          IconButton(
            tooltip: 'Synchroniser',
            icon: syncState.isSyncing
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : Icon(
                    Icons.sync,
                    color: isOnline ? Colors.white : Colors.white38,
                  ),
            onPressed: syncState.isSyncing ? null : () => syncNotifier.syncNow(),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () async {
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Status Card (Réseau & Synchro)
            Card(
              elevation: 2,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              color: isOnline ? Colors.green.shade50 : Colors.orange.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Icon(
                      isOnline ? Icons.cloud_done : Icons.cloud_off,
                      size: 32,
                      color: isOnline ? Colors.green.shade700 : Colors.orange.shade700,
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            isOnline ? 'Connexion établie' : 'Mode Hors-ligne',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                              color: isOnline ? Colors.green.shade900 : Colors.orange.shade900,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            isOnline
                                ? 'La synchronisation automatique est active.'
                                : 'Vos collectes sont sauvegardées localement.',
                            style: TextStyle(
                              fontSize: 13,
                              color: isOnline ? Colors.green.shade800 : Colors.orange.shade800,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 24),

            // Boutons d'action principaux
            ElevatedButton.icon(
              icon: const Icon(Icons.assignment_add),
              label: const Text('Nouvelle collecte'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: () => context.push('/forms'),
            ),

            const SizedBox(height: 12),

            OutlinedButton.icon(
              icon: const Icon(Icons.sync),
              label: const Text('Centre de synchronisation'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: () => ref.read(mainNavIndexProvider.notifier).state = 1,
            ),

            const SizedBox(height: 12),

            OutlinedButton.icon(
              icon: const Icon(Icons.format_list_bulleted),
              label: const Text('Liste des formulaires téléchargeables'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: () => context.push('/forms'),
            ),
          ],
        ),
      ),
    );
  }
}