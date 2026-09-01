import '../models/form.dart';
import 'api_client.dart';

class FormService {
  final ApiClient _apiClient;

  FormService(this._apiClient);

  /// Formulaires publiés de l'organisation de l'agent connecté
  /// (GET /forms/published — voir FormController.java).
  Future<List<FormSummary>> getPublishedForms() async {
    final response = await _apiClient.dio.get('/forms/published');
    final data = response.data as List;
    return data.map((json) => FormSummary.fromJson(json as Map<String, dynamic>)).toList();
  }

  /// Dernière version d'un formulaire (GET /forms/{id}/versions renvoie la
  /// liste triée par versionNumber décroissant côté backend — le premier
  /// élément est donc la dernière version).
  Future<FormVersionModel> getLatestVersion(int formId) async {
    final response = await _apiClient.dio.get('/forms/$formId/versions');
    final data = response.data as List;
    if (data.isEmpty) {
      throw Exception('Ce formulaire ne possède aucune version publiée');
    }
    return FormVersionModel.fromJson(data.first as Map<String, dynamic>);
  }

  /// Récupère une version spécifique d'un formulaire par son id
  /// (GET /form-versions/{id}).
  Future<FormVersionModel> getFormVersion(int versionId) async {
    final response = await _apiClient.dio.get('/form-versions/$versionId');
    return FormVersionModel.fromJson(response.data as Map<String, dynamic>);
  }
}
