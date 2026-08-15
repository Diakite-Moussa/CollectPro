/// Convention de schéma pour FormVersion.schemaJson (backend : String JSON
/// libre, non typé côté Java — voir FormVersion.java / CreateFormVersionRequest).
/// Ce fichier définit et interprète cette convention côté mobile.
///
/// Format attendu :
/// {
///   "fields": [
///     { "key": "nom_patient", "label": "Nom du patient", "type": "text", "required": true },
///     { "key": "statut_sante", "label": "Statut", "type": "select",
///       "options": ["Bon", "Moyen", "Critique"], "required": true }
///   ]
/// }
library;

import 'dart:convert';

enum FormFieldType {
  text,
  textarea,
  number,
  date,
  select,
  photo,
  document;

  static FormFieldType fromString(String raw) {
    return FormFieldType.values.firstWhere(
      (t) => t.name == raw,
      orElse: () => FormFieldType.text,
    );
  }
}

class FormFieldDef {
  final String key;
  final String label;
  final FormFieldType type;
  final bool required;
  final List<String>? options;

  FormFieldDef({
    required this.key,
    required this.label,
    required this.type,
    required this.required,
    this.options,
  });

  factory FormFieldDef.fromJson(Map<String, dynamic> json, [int index = 0]) {
    final key = json['key'] as String? ?? 'field_${index + 1}';
    return FormFieldDef(
      key: key,
      label: json['label'] as String? ?? key,
      type: FormFieldType.fromString(json['type'] as String? ?? 'text'),
      required: json['required'] as bool? ?? false,
      options: (json['options'] as List?)?.map((e) => e.toString()).toList(),
    );
  }
}

class FormSchema {
  final List<FormFieldDef> fields;

  FormSchema({required this.fields});

  factory FormSchema.fromJsonString(String schemaJson) {
    final decoded = jsonDecode(schemaJson) as Map<String, dynamic>;
    final rawFields = decoded['fields'] as List? ?? [];
    return FormSchema(
      fields: rawFields
          .asMap()
          .entries
          .map((entry) => FormFieldDef.fromJson(entry.value as Map<String, dynamic>, entry.key))
          .toList(),
    );
  }
}
