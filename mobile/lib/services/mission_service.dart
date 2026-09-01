import '../models/mission.dart';
import 'api_client.dart';

class MissionService {
  final ApiClient _apiClient;

  MissionService(this._apiClient);

  /// Missions assignées à l'agent connecté (GET /missions — le backend
  /// filtre automatiquement selon le rôle, voir MissionController.java).
  Future<List<MissionSummary>> getMyMissions() async {
    final response = await _apiClient.dio.get('/missions');
    final data = response.data as List;
    return data.map((json) => MissionSummary.fromJson(json as Map<String, dynamic>)).toList();
  }
}