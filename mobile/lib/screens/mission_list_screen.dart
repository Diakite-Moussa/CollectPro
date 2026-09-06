import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/mission.dart';
import '../providers/auth_provider.dart';
import '../services/mission_service.dart';
import 'collecte_form_screen.dart';
import 'form_list_screen.dart' show formServiceProvider;

final missionServiceProvider = Provider((ref) => MissionService(ref.watch(apiClientProvider)));

final myMissionsProvider = FutureProvider.autoDispose<List<MissionSummary>>((ref) {
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
          // Filtrer les missions actives et terminées
          final relevantMissions = missions
              .where((m) => m.status == 'ACTIVE' || m.status == 'COMPLETED')
              .toList();

          // Trier : Missions actives en premier, puis terminées
          relevantMissions.sort((a, b) {
            if (a.isCompleted == b.isCompleted) return 0;
            return a.isCompleted ? 1 : -1;
          });

          if (relevantMissions.isEmpty) {
            return RefreshIndicator(
              onRefresh: () => ref.refresh(myMissionsProvider.future),
              child: ListView(
                children: const [
                  SizedBox(height: 200),
                  Center(child: Text('Aucune mission pour le moment')),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => ref.refresh(myMissionsProvider.future),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: relevantMissions.length,
              itemBuilder: (context, index) {
                final mission = relevantMissions[index];
                final isCompleted = mission.isCompleted;

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  elevation: isCompleted ? 0 : 2,
                  color: isCompleted ? Colors.grey.shade100 : Theme.of(context).cardColor,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                    side: BorderSide(
                      color: isCompleted ? Colors.grey.shade300 : Colors.transparent,
                      width: 1,
                    ),
                  ),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: isCompleted
                        ? null // Incliquable si déjà fait
                        : () => _onMissionTap(context, ref, mission),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Expanded(
                                      child: Text(
                                        mission.name,
                                        style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 16,
                                          color: isCompleted ? Colors.grey.shade600 : null,
                                        ),
                                      ),
                                    ),
                                    if (isCompleted)
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                        decoration: BoxDecoration(
                                          color: Colors.green.shade50,
                                          border: Border.all(color: Colors.green.shade300),
                                          borderRadius: BorderRadius.circular(12),
                                        ),
                                        child: Row(
                                          mainAxisSize: MainAxisSize.min,
                                          children: [
                                            Icon(Icons.check_circle, size: 14, color: Colors.green.shade700),
                                            const SizedBox(width: 4),
                                            Text(
                                              'Déjà fait',
                                              style: TextStyle(
                                                fontSize: 12,
                                                fontWeight: FontWeight.w600,
                                                color: Colors.green.shade800,
                                              ),
                                            ),
                                          ],
                                        ),
                                      )
                                    else
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                        decoration: BoxDecoration(
                                          color: Colors.blue.shade50,
                                          border: Border.all(color: Colors.blue.shade200),
                                          borderRadius: BorderRadius.circular(12),
                                        ),
                                        child: Text(
                                          'En cours',
                                          style: TextStyle(
                                            fontSize: 12,
                                            fontWeight: FontWeight.w600,
                                            color: Colors.blue.shade800,
                                          ),
                                        ),
                                      ),
                                  ],
                                ),
                                if (mission.description != null) ...[
                                  const SizedBox(height: 6),
                                  Text(
                                    mission.description!,
                                    style: TextStyle(
                                      fontSize: 13,
                                      color: isCompleted ? Colors.grey.shade500 : Colors.grey.shade700,
                                    ),
                                  ),
                                ],
                                const SizedBox(height: 10),
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
                                const SizedBox(height: 6),
                                Row(
                                  children: [
                                    Icon(Icons.description_outlined, size: 16, color: Colors.grey.shade600),
                                    const SizedBox(width: 6),
                                    Text(
                                      '${mission.forms.length} formulaire(s) associé(s)',
                                      style: TextStyle(
                                        fontSize: 13,
                                        color: isCompleted ? Colors.grey.shade500 : Colors.grey.shade700,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          if (!isCompleted)
                            Padding(
                              padding: const EdgeInsets.only(top: 2),
                              child: Icon(Icons.chevron_right, color: Colors.grey.shade600),
                            ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }

  void _onMissionTap(BuildContext context, WidgetRef ref, MissionSummary mission) {
    if (mission.forms.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Aucun formulaire n\'est assigné à cette mission.'),
        ),
      );
      return;
    }

    _showMissionFormsBottomSheet(context, ref, mission);
  }

  void _showMissionFormsBottomSheet(BuildContext context, WidgetRef ref, MissionSummary mission) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (bottomSheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Colors.grey.shade300,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.blue.shade50,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(Icons.flag, color: Colors.blue.shade700),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            mission.name,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                          ),
                          Text(
                            'Sélectionnez un formulaire à remplir :',
                            style: TextStyle(fontSize: 13, color: Colors.grey.shade600),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                const Divider(),
                Flexible(
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: mission.forms.length,
                    separatorBuilder: (context, index) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final form = mission.forms[index];
                      return ListTile(
                        contentPadding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                        leading: CircleAvatar(
                          backgroundColor: Colors.blue.shade50,
                          child: Icon(Icons.assignment_outlined, color: Colors.blue.shade700, size: 20),
                        ),
                        title: Text(
                          form.name,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        subtitle: const Text('Toucher pour commencer la collecte'),
                        trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                        onTap: () {
                          Navigator.of(bottomSheetContext).pop();
                          _openFormForMission(context, ref, form.id, form.name, mission.id);
                        },
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _openFormForMission(
    BuildContext context,
    WidgetRef ref,
    int formId,
    String formName,
    int missionId,
  ) async {
    try {
      final version = await ref.read(formServiceProvider).getLatestVersion(formId);
      if (!context.mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => CollecteFormScreen(
            formVersionId: version.id,
            formTitle: formName,
            schemaJson: version.schemaJson,
            initialMissionId: missionId,
          ),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Erreur lors du chargement du formulaire : $e')),
      );
    }
  }
}