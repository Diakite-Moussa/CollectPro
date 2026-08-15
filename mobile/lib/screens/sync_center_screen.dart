import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../data/local/database.dart';
import '../providers/database_provider.dart';
import '../providers/sync_provider.dart';
import '../theme/app_theme.dart';

class SyncCenterScreen extends ConsumerWidget {
  const SyncCenterScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dao = ref.watch(collecteDaoProvider);
    final syncState = ref.watch(syncControllerProvider);
    final syncNotifier = ref.read(syncControllerProvider.notifier);
    final isOnlineAsync = ref.watch(isOnlineStreamProvider);
    final isOnline = isOnlineAsync.value ?? false;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Collectes & Synchronisation'),
        centerTitle: true,
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.primary,
        icon: const Icon(Icons.add),
        label: const Text('Nouvelle collecte'),
        onPressed: () => context.push('/forms'),
      ),
      body: StreamBuilder<List<CollecteRow>>(
        stream: dao.watchAllCollectes(),
        builder: (context, snapshot) {
          final collectes = snapshot.data ?? [];
          final pending = collectes.where((c) => c.localStatus == 'PENDING_SYNC').toList();
          final drafts = collectes.where((c) => c.localStatus == 'DRAFT').toList();
          final failed = collectes.where((c) => c.localStatus == 'SYNC_FAILED').toList();
          final synced = collectes.where((c) => c.localStatus == 'SYNCED').toList();

          return SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 88),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
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
                          size: 36,
                          color: isOnline ? Colors.green.shade700 : Colors.orange.shade700,
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                isOnline ? 'Connecté au serveur' : 'Mode Hors-ligne',
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
                                    : 'Les saisies sont sauvegardées localement sur SQLite.',
                                style: TextStyle(
                                  fontSize: 12,
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

                const SizedBox(height: 20),

                Card(
                  elevation: 2,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text(
                              'Données en attente',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                            ),
                            Chip(
                              label: Text('${pending.length + failed.length} éléments'),
                              backgroundColor: (pending.isEmpty && failed.isEmpty)
                                  ? Colors.grey.shade100
                                  : AppColors.warning.withOpacity(0.2),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        if (syncState.isSyncing) ...[
                          const LinearProgressIndicator(),
                          const SizedBox(height: 12),
                          const Text(
                            'Synchronisation en cours avec le serveur...',
                            style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.bold),
                          ),
                        ] else ...[
                          ElevatedButton.icon(
                            icon: const Icon(Icons.sync),
                            label: const Text('Synchroniser maintenant'),
                            style: ElevatedButton.styleFrom(
                              minimumSize: const Size.fromHeight(50),
                            ),
                            onPressed: isOnline
                                ? () async {
                                    await dao.retryAllFailedCollectes();
                                    syncNotifier.syncNow();
                                  }
                                : null,
                          ),
                        ],
                        if (syncState.lastSyncTime != null) ...[
                          const SizedBox(height: 10),
                          Text(
                            'Dernière synchro : ${syncState.lastSyncTime!.toLocal().toString().split('.')[0]}',
                            style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 24),

                if (pending.isNotEmpty) ...[
                  Text(
                    'En attente de synchronisation (${pending.length})',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                  const SizedBox(height: 8),
                  ...pending.map((c) => _CollecteListTile(
                        collecte: c,
                        icon: Icons.cloud_upload,
                        iconColor: AppColors.primary,
                        onTap: () => context.push('/collecte-detail/${c.id}'),
                      )),
                  const SizedBox(height: 16),
                ],

                if (drafts.isNotEmpty) ...[
                  Text(
                    'Brouillons (${drafts.length})',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.grey.shade700),
                  ),
                  const SizedBox(height: 8),
                  ...drafts.map((c) => _CollecteListTile(
                        collecte: c,
                        icon: Icons.edit_note,
                        iconColor: Colors.grey.shade600,
                        onTap: () => context.push('/collecte-detail/${c.id}'),
                      )),
                  const SizedBox(height: 16),
                ],

                if (failed.isNotEmpty) ...[
                  Text(
                    'Collectes en échec (${failed.length})',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.red),
                  ),
                  const SizedBox(height: 8),
                  ...failed.map((c) => Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          leading: const Icon(Icons.error, color: Colors.red),
                          title: Text('Collecte #${c.id} (Version ${c.formVersionId})'),
                          subtitle: Text(c.syncErrorMessage ?? 'Erreur inconnue'),
                          onTap: () => context.push('/collecte-detail/${c.id}'),
                          trailing: IconButton(
                            icon: const Icon(Icons.refresh, color: Colors.red),
                            onPressed: () async {
                              await dao.retryFailedCollecte(c.id);
                              syncNotifier.syncNow();
                            },
                          ),
                        ),
                      )),
                  const SizedBox(height: 16),
                ],

                Text(
                  'Historique récent (${synced.length} synchronisées)',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(height: 8),
                if (synced.isEmpty)
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Aucune collecte synchronisée pour le moment.',
                      style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
                      textAlign: TextAlign.center,
                    ),
                  )
                else
                  ...synced.take(10).map((c) => Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          leading: const Icon(Icons.check_circle, color: Colors.green),
                          title: Text('Collecte #${c.id} ➔ Serveur #${c.serverId}'),
                          subtitle: Text('Sync le ${c.updatedAt.toLocal().toString().split('.')[0]}'),
                          onTap: () => context.push('/collecte-detail/${c.id}'),
                        ),
                      )),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _CollecteListTile extends StatelessWidget {
  final CollecteRow collecte;
  final IconData icon;
  final Color iconColor;
  final VoidCallback onTap;

  const _CollecteListTile({
    required this.collecte,
    required this.icon,
    required this.iconColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(icon, color: iconColor),
        title: Text('Collecte #${collecte.id} (Formulaire v${collecte.formVersionId})'),
        subtitle: Text(
          'Créée le ${collecte.createdAt.toLocal().toString().split('.')[0]}',
          style: const TextStyle(fontSize: 12),
        ),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }
}
