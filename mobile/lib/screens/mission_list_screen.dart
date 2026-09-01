import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/mission.dart';
import '../providers/auth_provider.dart';
import '../services/mission_service.dart';

final missionServiceProvider = Provider((ref) => MissionService(ref.watch(apiClientProvider)));

final myMissionsProvider = FutureProvider<List<MissionSummary>>((ref) {
  return ref.watch(missionServiceProvider).getMyMissions();
});


String _formatDate(DateTime date) {
  final day = date.day.toString().padLeft(2, '0');
  final month = date.month.toString().padLeft(2, '0');
  return '$day/$month/${date.year}';
}

String _formatEcheance(MissionSummary mission) {
  if (mission.startDate == null && mission.endDate == null) {
    return 'Aucune échéance définie';
  }
  if (mission.startDate != null && mission.endDate != null) {
    return '${_formatDate(mission.startDate!)} → ${_formatDate(mission.endDate!)}';
  }
  if (mission.endDate != null) {
    return "Jusqu'au ${_formatDate(mission.endDate!)}";
  }
  return 'Depuis le ${_formatDate(mission.startDate!)}';
}

/// Missions assignées à l'agent connecté (Sprint 2 V1.1 — RF-MISSION).
class MissionListScreen extends ConsumerWidget {
  const MissionListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final missionsAsync = ref.watch(myMissionsProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Mes missions')),
      body: missionsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: Colors.red),
                const SizedBox(height: 12),
                Text('Impossible de charger les missions\n$error', textAlign: TextAlign.center),
                const SizedBox(height: 12),
                ElevatedButton(
                  onPressed: () => ref.invalidate(myMissionsProvider),
                  child: const Text('Réessayer'),
                ),
              ],
            ),
          ),
        ),
        data: (missions) {
          final activeMissions = missions.where((m) => m.status == 'ACTIVE').toList();
          if (activeMissions.isEmpty) {
            return const Center(child: Text('Aucune mission active pour le moment'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: activeMissions.length,
            itemBuilder: (context, index) {
              final mission = activeMissions[index];
              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              mission.name,
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                            ),
                            if (mission.description != null) ...[
                              const SizedBox(height: 4),
                              Text(
                                mission.description!,
                                style: TextStyle(fontSize: 13, color: Colors.grey.shade700),
                              ),
                            ],
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Icon(Icons.event, size: 16, color: Colors.grey.shade600),
                                const SizedBox(width: 6),
                                Text(
                                  _formatEcheance(mission),
                                  style: TextStyle(fontSize: 13, color: Colors.grey.shade700),
                                ),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                Icon(Icons.description_outlined, size: 16, color: Colors.grey.shade600),
                                const SizedBox(width: 6),
                                Text(
                                  '${mission.forms.length} formulaire(s) associé(s)',
                                  style: TextStyle(fontSize: 13, color: Colors.grey.shade700),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const Icon(Icons.flag_outlined),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}