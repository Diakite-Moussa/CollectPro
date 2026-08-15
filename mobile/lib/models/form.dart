class FormSummary {
  final int id;
  final String name;
  final String? description;
  final String status;
  final int? latestVersionNumber;

  FormSummary({
    required this.id,
    required this.name,
    this.description,
    required this.status,
    this.latestVersionNumber,
  });

  factory FormSummary.fromJson(Map<String, dynamic> json) {
    return FormSummary(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      status: json['status'] as String,
      latestVersionNumber: json['latestVersionNumber'] as int?,
    );
  }
}

class FormVersionModel {
  final int id;
  final int formId;
  final int versionNumber;
  final String schemaJson;

  FormVersionModel({
    required this.id,
    required this.formId,
    required this.versionNumber,
    required this.schemaJson,
  });

  factory FormVersionModel.fromJson(Map<String, dynamic> json) {
    return FormVersionModel(
      id: json['id'] as int,
      formId: json['formId'] as int,
      versionNumber: json['versionNumber'] as int,
      schemaJson: json['schemaJson'] as String,
    );
  }
}
