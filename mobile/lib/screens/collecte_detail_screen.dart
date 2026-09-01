import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../data/local/database.dart';
import '../providers/database_provider.dart';
import '../providers/sync_provider.dart';
import '../theme/app_theme.dart';

class CollecteDetailScreen extends ConsumerWidget {
  final int localId;

  const CollecteDetailScreen({
    super.key,
    required this.localId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dao = ref.watch(collecteDaoProvider);
    final syncNotifier = ref.read(syncControllerProvider.notifier);
    final syncState = ref.watch(syncControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text('Collecte #$localId'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline, color: Colors.white),
            tooltip: 'Supprimer',
            onPressed: () async {
              final confirm = await showDialog<bool>(
                context: context,
                builder: (ctx) => AlertDialog(
                  title: const Text('Supprimer cette collecte ?'),
                  content: const Text('Cette action est irréversible.'),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(ctx, false),
                      child: const Text('Annuler'),
                    ),
                    TextButton(
                      style: TextButton.styleFrom(foregroundColor: Colors.red),
                      onPressed: () => Navigator.pop(ctx, true),
                      child: const Text('Supprimer'),
                    ),
                  ],
                ),
              );
              if (confirm == true) {
                await dao.deleteCollecte(localId);
                if (context.mounted) context.pop();
              }
            },
          ),
        ],
      ),
      body: StreamBuilder<List<CollecteRow>>(
        stream: dao.watchAllCollectes(),
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return const Center(child: CircularProgressIndicator());
          }

          final list = snapshot.data!;
          final matches = list.where((c) => c.id == localId);
          if (matches.isEmpty) {
            return const Center(
              child: Text('Cette collecte n\'existe plus.'),
            );
          }

          final c = matches.first;

          // Décodage des réponses JSON
          Map<String, dynamic> dataMap = {};
          try {
            dataMap = jsonDecode(c.dataJson) as Map<String, dynamic>;
          } catch (_) {}

          // Décodage des photos
          List<String> photos = [];
          if (c.photoPaths != null && c.photoPaths!.isNotEmpty) {
            try {
              final decoded = jsonDecode(c.photoPaths!);
              if (decoded is List) {
                photos = decoded.map((e) => e.toString()).toList();
              }
            } catch (_) {}
          }

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // En-tête Statut & ID Serveur
                Card(
                  elevation: 2,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Formulaire V${c.formVersionId}',
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            _StatusBadge(status: c.serverStatus ?? c.localStatus),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Créée le : ${c.createdAt.toLocal().toString().split('.')[0]}',
                          style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
                        ),
                        if (c.updatedAt != c.createdAt)
                          Text(
                            'Mise à jour : ${c.updatedAt.toLocal().toString().split('.')[0]}',
                            style: TextStyle(color: Colors.grey.shade500, fontSize: 12),
                          ),
                        if (c.serverId != null) ...[
                          const SizedBox(height: 10),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            decoration: BoxDecoration(
                              color: Colors.green.shade50,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(color: Colors.green.shade200),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(Icons.cloud_done, size: 16, color: Colors.green),
                                const SizedBox(width: 6),
                                Text(
                                  'Synchronisée avec le serveur (ID #${c.serverId})',
                                  style: TextStyle(
                                    color: Colors.green.shade900,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 13,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                        if (c.syncErrorMessage != null) ...[
                          const SizedBox(height: 10),
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: Colors.red.shade50,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(color: Colors.red.shade200),
                            ),
                            child: Row(
                              children: [
                                Icon(Icons.error_outline, color: Colors.red.shade700, size: 18),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    c.syncErrorMessage!,
                                    style: TextStyle(color: Colors.red.shade900, fontSize: 12),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),

                if (c.serverStatus == 'REJECTED' && c.validationComment != null) ...[
                  const SizedBox(height: 16),
                  Card(
                    elevation: 1,
                    color: Colors.deepOrange.shade50,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(Icons.comment, color: Colors.deepOrange.shade700),
                              const SizedBox(width: 8),
                              Text(
                                'Motif du rejet${c.validatedByName != null ? ' — ${c.validatedByName}' : ''}',
                                style: TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.deepOrange.shade900,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(
                            c.validationComment!,
                            style: TextStyle(color: Colors.deepOrange.shade900),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],

                const SizedBox(height: 16),

                // Carte Géolocalisation GPS
                Card(
                  elevation: 1,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.location_on, color: AppColors.primary),
                            SizedBox(width: 8),
                            Text(
                              'Coordonnées GPS',
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        if (c.latitude != null && c.longitude != null) ...[
                          Row(
                            children: [
                              Expanded(
                                child: Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: Colors.grey.shade100,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text('Latitude', style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
                                      const SizedBox(height: 2),
                                      Text(
                                        '${c.latitude!.toStringAsFixed(6)}°',
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: Colors.grey.shade100,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text('Longitude', style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
                                      const SizedBox(height: 2),
                                      Text(
                                        '${c.longitude!.toStringAsFixed(6)}°',
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ] else ...[
                          Text(
                            'Aucune donnée GPS enregistrée pour cette collecte.',
                            style: TextStyle(color: Colors.grey.shade600, fontStyle: FontStyle.italic),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 16),

                // Photos & Pièces jointes
                if (photos.isNotEmpty) ...[
                  Card(
                    elevation: 1,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.photo_library, color: AppColors.primary),
                              const SizedBox(width: 8),
                              Text(
                                'Photos jointes (${photos.length})',
                                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          SizedBox(
                            height: 110,
                            child: ListView.builder(
                              scrollDirection: Axis.horizontal,
                              itemCount: photos.length,
                              itemBuilder: (context, idx) {
                                final path = photos[idx];
                                final fileExists = File(path).existsSync();
                                return Padding(
                                  padding: const EdgeInsets.only(right: 12),
                                  child: ClipRRect(
                                    borderRadius: BorderRadius.circular(12),
                                    child: fileExists
                                        ? Image.file(
                                            File(path),
                                            width: 110,
                                            height: 110,
                                            fit: BoxFit.cover,
                                          )
                                        : Container(
                                            width: 110,
                                            height: 110,
                                            color: Colors.grey.shade200,
                                            child: const Icon(Icons.broken_image, color: Colors.grey),
                                          ),
                                  ),
                                );
                              },
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                ],

                // Données du Formulaire
                Card(
                  elevation: 1,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.assignment, color: AppColors.primary),
                            SizedBox(width: 8),
                            Text(
                              'Réponses saisies',
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        if (dataMap.isEmpty)
                          Text(
                            c.dataJson,
                            style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
                          )
                        else
                          Column(
                            children: dataMap.entries.map((entry) {
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 8),
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Expanded(
                                      flex: 2,
                                      child: Text(
                                        '${entry.key} :',
                                        style: TextStyle(
                                          fontWeight: FontWeight.w600,
                                          color: Colors.grey.shade700,
                                        ),
                                      ),
                                    ),
                                    Expanded(
                                      flex: 3,
                                      child: Text(
                                        '${entry.value}',
                                        style: const TextStyle(fontWeight: FontWeight.w500),
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            }).toList(),
                          ),
                      ],
                    ),
                  ),
                ),

                const SizedBox(height: 24),

                // Boutons d'action
                if (c.serverStatus == 'REJECTED') ...[
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      icon: const Icon(Icons.replay),
                      label: const Text('Corriger et renvoyer'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.deepOrange,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: () {
                        context.push('/forms/edit-rejected/${c.id}');
                      },
                    ),
                  ),
                ] else if (c.localStatus == 'SYNC_FAILED' || c.localStatus == 'DRAFT' || c.localStatus == 'PENDING_SYNC') ...[
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      icon: syncState.isSyncing
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                            )
                          : const Icon(Icons.sync),
                      label: Text(c.localStatus == 'SYNC_FAILED' ? 'Réessayer la synchronisation' : 'Synchroniser maintenant'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: syncState.isSyncing
                          ? null
                          : () async {
                              if (c.localStatus == 'SYNC_FAILED' || c.localStatus == 'DRAFT') {
                                await dao.retryFailedCollecte(c.id);
                              }
                              await syncNotifier.syncNow();
                            },
                    ),
                  ),
                  if (c.localStatus == 'SYNC_FAILED' || c.localStatus == 'DRAFT') ...[
                    const SizedBox(height: 10),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        icon: const Icon(Icons.edit),
                        label: const Text('Modifier les réponses'),
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        onPressed: () {
                          context.push('/forms');
                        },
                      ),
                    ),
                  ],
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String status;

  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    Color bgColor;
    Color textColor;
    IconData icon;
    String label;

    switch (status) {
      case 'DRAFT':
        bgColor = Colors.grey.shade200;
        textColor = Colors.grey.shade800;
        icon = Icons.edit_note;
        label = 'BROUILLON';
        break;
      case 'PENDING_SYNC':
        bgColor = Colors.amber.shade100;
        textColor = Colors.amber.shade900;
        icon = Icons.schedule;
        label = 'EN ATTENTE SYNC';
        break;
      case 'SYNCING':
        bgColor = Colors.blue.shade100;
        textColor = Colors.blue.shade900;
        icon = Icons.sync;
        label = 'SYNCHRONISATION...';
        break;
      case 'SYNCED':
        bgColor = Colors.green.shade100;
        textColor = Colors.green.shade900;
        icon = Icons.check_circle_outline;
        label = 'SYNCHRONISÉE';
        break;
      case 'SYNC_FAILED':
        bgColor = Colors.red.shade100;
        textColor = Colors.red.shade900;
        icon = Icons.error_outline;
        label = 'ÉCHEC SYNCHRO';
        break;
      case 'PENDING_VALIDATION':
        bgColor = Colors.blue.shade50;
        textColor = Colors.blue.shade800;
        icon = Icons.hourglass_top;
        label = 'EN ATTENTE DE VALIDATION';
        break;
      case 'VALIDATED':
        bgColor = Colors.teal.shade100;
        textColor = Colors.teal.shade900;
        icon = Icons.verified;
        label = 'VALIDÉE';
        break;
      case 'REJECTED':
        bgColor = Colors.deepOrange.shade100;
        textColor = Colors.deepOrange.shade900;
        icon = Icons.cancel_outlined;
        label = 'REJETÉE';
        break;
      default:
        bgColor = Colors.grey.shade200;
        textColor = Colors.black;
        icon = Icons.help_outline;
        label = status;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: textColor),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              color: textColor,
              fontWeight: FontWeight.bold,
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }
}
