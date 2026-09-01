class MissionSummary {
  final int id;
  final String name;
  final String? description;
  final String status;
  final DateTime? startDate;
  final DateTime? endDate;
  final double? latitude;
  final double? longitude;
  final double? radiusMeters;
  final List<MissionFormRef> forms;

  MissionSummary({
    required this.id,
    required this.name,
    this.description,
    required this.status,
    this.startDate,
    this.endDate,
    this.latitude,
    this.longitude,
    this.radiusMeters,
    required this.forms,
  });

  factory MissionSummary.fromJson(Map<String, dynamic> json) {
    return MissionSummary(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      status: json['status'] as String,
      startDate: json['startDate'] != null ? DateTime.parse(json['startDate'] as String) : null,
      endDate: json['endDate'] != null ? DateTime.parse(json['endDate'] as String) : null,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      radiusMeters: (json['radiusMeters'] as num?)?.toDouble(),
      forms: (json['forms'] as List? ?? [])
          .map((f) => MissionFormRef.fromJson(f as Map<String, dynamic>))
          .toList(),
    );
  }
}

class MissionFormRef {
  final int id;
  final String name;

  MissionFormRef({required this.id, required this.name});

  factory MissionFormRef.fromJson(Map<String, dynamic> json) {
    return MissionFormRef(id: json['id'] as int, name: json['name'] as String);
  }
}