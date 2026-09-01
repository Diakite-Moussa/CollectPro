import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/local/database.dart';
import '../models/form.dart';
import '../providers/database_provider.dart';
import 'collecte_form_screen.dart';
import 'form_list_screen.dart';

class EditRejectedCollecteScreen extends ConsumerWidget {
  final int localId;

  const EditRejectedCollecteScreen({
    super.key,
    required this.localId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dao = ref.watch(collecteDaoProvider);
    final formService = ref.watch(formServiceProvider);

    return FutureBuilder<CollecteRow?>(
      future: dao.getById(localId),
      builder: (context, collecteSnapshot) {
        if (collecteSnapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }

        final collecte = collecteSnapshot.data;
        if (collecte == null) {
          return Scaffold(
            appBar: AppBar(title: const Text('Correction de la collecte')),
            body: const Center(child: Text('Collecte introuvable.')),
          );
        }

        return FutureBuilder<FormVersionModel>(
          future: formService.getFormVersion(collecte.formVersionId),
          builder: (context, versionSnapshot) {
            if (versionSnapshot.connectionState == ConnectionState.waiting) {
              return const Scaffold(
                body: Center(child: CircularProgressIndicator()),
              );
            }

            if (versionSnapshot.hasError || !versionSnapshot.hasData) {
              return Scaffold(
                appBar: AppBar(title: const Text('Correction de la collecte')),
                body: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Impossible de charger la structure du formulaire : ${versionSnapshot.error}',
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              );
            }

            final version = versionSnapshot.data!;

            Map<String, dynamic> initialAnswers = {};
            try {
              initialAnswers = jsonDecode(collecte.dataJson) as Map<String, dynamic>;
            } catch (_) {}

            List<String> photoPaths = [];
            if (collecte.photoPaths != null && collecte.photoPaths!.isNotEmpty) {
              try {
                final decoded = jsonDecode(collecte.photoPaths!);
                if (decoded is List) {
                  photoPaths = decoded.map((e) => e.toString()).toList();
                }
              } catch (_) {}
            }

            List<String> documentPaths = [];
            if (collecte.documentPaths != null && collecte.documentPaths!.isNotEmpty) {
              try {
                final decoded = jsonDecode(collecte.documentPaths!);
                if (decoded is List) {
                  documentPaths = decoded.map((e) => e.toString()).toList();
                }
              } catch (_) {}
            }

            return CollecteFormScreen(
              formVersionId: version.id,
              formTitle: 'Correction collecte #${collecte.id}',
              schemaJson: version.schemaJson,
              localCollecteId: collecte.id,
              serverCollecteId: collecte.serverId,
              initialAnswers: initialAnswers,
              initialLatitude: collecte.latitude,
              initialLongitude: collecte.longitude,
              initialPhotoPaths: photoPaths,
              initialDocumentPaths: documentPaths,
              isResubmit: true,
            );
          },
        );
      },
    );
  }
}
