import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/form.dart';
import '../providers/auth_provider.dart';
import '../services/form_service.dart';
import 'collecte_form_screen.dart';

final formServiceProvider = Provider((ref) => FormService(ref.watch(apiClientProvider)));

final publishedFormsProvider = FutureProvider<List<FormSummary>>((ref) {
  return ref.watch(formServiceProvider).getPublishedForms();
});

/// Liste des formulaires publiés (RF-014 : "les Agents reçoivent uniquement
/// les formulaires auxquels ils sont autorisés" — filtré côté backend par
/// organisation, cf. GET /forms/published).
class FormListScreen extends ConsumerWidget {
  const FormListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final formsAsync = ref.watch(publishedFormsProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Formulaires disponibles')),
      body: formsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: Colors.red),
                const SizedBox(height: 12),
                Text('Impossible de charger les formulaires\n$error', textAlign: TextAlign.center),
                const SizedBox(height: 12),
                ElevatedButton(
                  onPressed: () => ref.invalidate(publishedFormsProvider),
                  child: const Text('Réessayer'),
                ),
              ],
            ),
          ),
        ),
        data: (forms) {
          if (forms.isEmpty) {
            return const Center(child: Text('Aucun formulaire publié pour le moment'));
          }
          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: forms.length,
            itemBuilder: (context, index) {
              final form = forms[index];
              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: ListTile(
                  title: Text(form.name),
                  subtitle: Text(form.description ?? 'Version ${form.latestVersionNumber ?? '-'}'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _openForm(context, ref, form),
                ),
              );
            },
          );
        },
      ),
    );
  }

  Future<void> _openForm(BuildContext context, WidgetRef ref, FormSummary form) async {
    try {
      final version = await ref.read(formServiceProvider).getLatestVersion(form.id);
      if (!context.mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => CollecteFormScreen(
            formVersionId: version.id,
            formTitle: form.name,
            schemaJson: version.schemaJson,
          ),
        ),
      );
    } catch (e) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Erreur : $e')),
      );
    }
  }
}
