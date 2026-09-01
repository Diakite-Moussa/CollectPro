class CollecteStatusModel {
  final int id; // serverId
  final String status;
  final String? validationComment;
  final String? validatedByName;
  final DateTime? validatedAt;

  CollecteStatusModel({
    required this.id,
    required this.status,
    this.validationComment,
    this.validatedByName,
    this.validatedAt,
  });

  factory CollecteStatusModel.fromJson(Map<String, dynamic> json) {
    final validatedBy = json['validatedBy'] as Map<String, dynamic>?;
    return CollecteStatusModel(
      id: json['id'] as int,
      status: json['status'] as String,
      validationComment: json['validationComment'] as String?,
      validatedByName: validatedBy != null
          ? '${validatedBy['firstName']} ${validatedBy['lastName']}'
          : null,
      validatedAt: json['validatedAt'] != null
          ? DateTime.parse(json['validatedAt'] as String)
          : null,
    );
  }
}
