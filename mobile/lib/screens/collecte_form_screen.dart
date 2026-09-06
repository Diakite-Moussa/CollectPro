import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models/form_schema.dart';
import '../models/mission.dart';
import '../providers/auth_provider.dart';
import '../providers/database_provider.dart';
import '../providers/sync_provider.dart';
import '../screens/mission_list_screen.dart' show myMissionsProvider;
import '../services/location_service.dart';
import '../services/media_service.dart';
import '../theme/app_theme.dart';

/// Écran générique de saisie de collecte généré dynamiquement à partir
/// du schemaJson d'un FormVersion.
///
/// Intègre la capture automatique des coordonnées GPS (Geolocator)
/// et la prise de photo / sélection dans la galerie (ImagePicker).
class CollecteFormScreen extends ConsumerStatefulWidget {
  final int formVersionId;
  final String formTitle;
  final String schemaJson;
  final int? localCollecteId;
  final int? serverCollecteId;
  final Map<String, dynamic>? initialAnswers;
  final double? initialLatitude;
  final double? initialLongitude;
  final List<String>? initialPhotoPaths;
  final List<String>? initialDocumentPaths;
  final int? initialMissionId;
  final bool isResubmit;

  const CollecteFormScreen({
    super.key,
    required this.formVersionId,
    required this.formTitle,
    required this.schemaJson,
    this.localCollecteId,
    this.serverCollecteId,
    this.initialAnswers,
    this.initialLatitude,
    this.initialLongitude,
    this.initialPhotoPaths,
    this.initialDocumentPaths,
    this.initialMissionId,
    this.isResubmit = false,
  });

  @override
  ConsumerState<CollecteFormScreen> createState() => _CollecteFormScreenState();
}

class _CollecteFormScreenState extends ConsumerState<CollecteFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final FormSchema _schema;
  final Map<String, dynamic> _answers = {};
  bool _submitting = false;

  // GPS State
  final LocationService _locationService = LocationService();
  double? _latitude;
  double? _longitude;
  bool _fetchingGps = false;
  String? _gpsError;

  // Media State
  final MediaService _mediaService = MediaService();
  final List<String> _photoPaths = [];
  final List<String> _documentPaths = [];

  // Mission State
  int? _selectedMissionId;

  @override
  void initState() {
    super.initState();
    _schema = FormSchema.fromJsonString(widget.schemaJson);

    if (widget.initialMissionId != null) {
      _selectedMissionId = widget.initialMissionId;
    }

    if (widget.initialAnswers != null) {
      _answers.addAll(widget.initialAnswers!);
    }
    if (widget.initialLatitude != null && widget.initialLongitude != null) {
      _latitude = widget.initialLatitude;
      _longitude = widget.initialLongitude;
    }
    if (widget.initialPhotoPaths != null) {
      _photoPaths.addAll(widget.initialPhotoPaths!);
    }
    if (widget.initialDocumentPaths != null) {
      _documentPaths.addAll(widget.initialDocumentPaths!);
    }

    if (!widget.isResubmit && (_latitude == null || _longitude == null)) {
      _captureGps();
    }
  }

  Future<void> _captureGps() async {
    setState(() {
      _fetchingGps = true;
      _gpsError = null;
    });

    final position = await _locationService.getCurrentLocation();

    if (!mounted) return;
    if (position != null) {
      setState(() {
        _latitude = position.latitude;
        _longitude = position.longitude;
        _fetchingGps = false;
      });
    } else {
      setState(() {
        _fetchingGps = false;
        _gpsError = 'Impossible de récupérer la position GPS. Vérifiez les permissions.';
      });
    }
  }

  Future<void> _takePhoto() async {
    final path = await _mediaService.takePhoto();
    if (path != null && mounted) {
      setState(() {
        _photoPaths.add(path);
      });
    }
  }

  Future<void> _pickGallery() async {
    final path = await _mediaService.pickImageFromGallery();
    if (path != null && mounted) {
      setState(() {
        _photoPaths.add(path);
      });
    }
  }

  Future<void> _submit() async {
    final formState = _formKey.currentState;
    if (formState == null || !formState.validate()) return;
    formState.save();

    setState(() => _submitting = true);

    final dao = ref.read(collecteDaoProvider);
    final dataJson = jsonEncode(_answers);

    try {
      if (widget.isResubmit) {
        if (widget.serverCollecteId != null) {
          final syncService = ref.read(syncServiceProvider);
          await syncService.resubmitCollecte(
            serverId: widget.serverCollecteId!,
            dataJson: dataJson,
            latitude: _latitude,
            longitude: _longitude,
          );
        }

        if (widget.localCollecteId != null) {
          await dao.updateCollecte(
            localId: widget.localCollecteId!,
            dataJson: dataJson,
            latitude: _latitude,
            longitude: _longitude,
            photoPaths: _photoPaths.isNotEmpty ? _photoPaths : null,
            documentPaths: _documentPaths.isNotEmpty ? _documentPaths : null,
            readyForSync: false,
          );
          if (widget.serverCollecteId != null) {
            await dao.updateValidationStatus(
              serverId: widget.serverCollecteId!,
              serverStatus: 'PENDING_VALIDATION',
              validationComment: null,
            );
          }
        }

        if (!mounted) return;
        setState(() => _submitting = false);

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Collecte corrigée et renvoyée avec succès'),
            backgroundColor: Colors.green,
          ),
        );
        context.pop();
      } else {
        final agentId = ref.read(authControllerProvider).user?.id;

        await dao.insertCollecte(
          formVersionId: widget.formVersionId,
          dataJson: dataJson,
          agentId: agentId,
          missionId: _selectedMissionId,
          latitude: _latitude,
          longitude: _longitude,
          photoPaths: _photoPaths.isNotEmpty ? _photoPaths : null,
          documentPaths: _documentPaths.isNotEmpty ? _documentPaths : null,
          readyForSync: true,
        );

        if (!mounted) return;
        setState(() => _submitting = false);

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Collecte enregistrée localement (prête à synchroniser)'),
            backgroundColor: Colors.green,
          ),
        );
        context.pop();
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Erreur : $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.formTitle)),
      body: Form(
        key: _formKey,
        autovalidateMode: AutovalidateMode.onUserInteraction,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Banner GPS Status
            _buildGpsCard(),

            const SizedBox(height: 16),

            // Sélection de mission (optionnel)
            _buildMissionSelector(),

            const SizedBox(height: 16),

            // Dynamic Form Fields
            ..._schema.fields.map(_buildField),

            const SizedBox(height: 16),

            // Section Photos / Attachments
            _buildPhotoSection(),

            const SizedBox(height: 24),

            // Submit Button
            ElevatedButton.icon(
              icon: _submitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.send),
              label: Text(_submitting ? 'Enregistrement...' : 'Enregistrer la collecte'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: _submitting ? null : _submit,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGpsCard() {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      color: _latitude != null ? Colors.green.shade50 : Colors.orange.shade50,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Icon(
              _latitude != null ? Icons.location_on : Icons.location_searching,
              color: _latitude != null ? Colors.green.shade700 : Colors.orange.shade700,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _latitude != null
                        ? 'Position GPS capturée'
                        : (_fetchingGps ? 'Capture de la position GPS...' : 'GPS non disponible'),
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                      color: _latitude != null ? Colors.green.shade900 : Colors.orange.shade900,
                    ),
                  ),
                  if (_latitude != null && _longitude != null)
                    Text(
                      'Lat: ${_latitude!.toStringAsFixed(6)}°, Lon: ${_longitude!.toStringAsFixed(6)}°',
                      style: TextStyle(fontSize: 12, color: Colors.green.shade800),
                    )
                  else if (_gpsError != null)
                    Text(
                      _gpsError!,
                      style: TextStyle(fontSize: 11, color: Colors.red.shade800),
                    ),
                ],
              ),
            ),
            IconButton(
              icon: _fetchingGps
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.refresh, size: 20),
              tooltip: 'Actualiser la position',
              onPressed: _fetchingGps ? null : _captureGps,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPhotoSection() {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.camera_alt_outlined, color: AppColors.primary),
                SizedBox(width: 8),
                Text(
                  'Photos et pièces jointes',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                OutlinedButton.icon(
                  icon: const Icon(Icons.photo_camera, size: 18),
                  label: const Text('Appareil photo'),
                  onPressed: _takePhoto,
                ),
                const SizedBox(width: 8),
                OutlinedButton.icon(
                  icon: const Icon(Icons.photo_library, size: 18),
                  label: const Text('Galerie'),
                  onPressed: _pickGallery,
                ),
              ],
            ),
            if (_photoPaths.isNotEmpty) ...[
              const SizedBox(height: 12),
              SizedBox(
                height: 90,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  itemCount: _photoPaths.length,
                  itemBuilder: (context, idx) {
                    final path = _photoPaths[idx];
                    return Stack(
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(right: 8, top: 4),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: File(path).existsSync()
                                ? Image.file(File(path), width: 80, height: 80, fit: BoxFit.cover)
                                : Container(
                                    width: 80,
                                    height: 80,
                                    color: Colors.grey.shade300,
                                    child: const Icon(Icons.broken_image),
                                  ),
                          ),
                        ),
                        Positioned(
                          top: 0,
                          right: 4,
                          child: InkWell(
                            onTap: () {
                              setState(() {
                                _photoPaths.removeAt(idx);
                              });
                            },
                            child: Container(
                              padding: const EdgeInsets.all(2),
                              decoration: const BoxDecoration(
                                color: Colors.red,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.close, size: 14, color: Colors.white),
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildField(FormFieldDef field) {
    switch (field.type) {
      case FormFieldType.text:
        return _textField(field, maxLines: 1, keyboardType: TextInputType.text);
      case FormFieldType.textarea:
        return _textField(field, maxLines: 4, keyboardType: TextInputType.multiline);
      case FormFieldType.number:
        return _textField(field, maxLines: 1, keyboardType: TextInputType.number, isNumber: true);
      case FormFieldType.date:
        return _dateField(field);
      case FormFieldType.select:
        return _selectField(field);
      case FormFieldType.photo:
        return _photoField(field);
      case FormFieldType.document:
        return _documentField(field);
    }
  }

  Widget _textField(
    FormFieldDef field, {
    required int maxLines,
    required TextInputType keyboardType,
    bool isNumber = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: TextFormField(
        initialValue: _answers[field.key]?.toString(),
        decoration: InputDecoration(labelText: field.label + (field.required ? ' *' : '')),
        maxLines: maxLines,
        keyboardType: keyboardType,
        validator: (value) {
          if (field.required && (value == null || value.trim().isEmpty)) {
            return 'Ce champ est obligatoire';
          }
          if (isNumber && value != null && value.isNotEmpty && num.tryParse(value) == null) {
            return 'Nombre invalide';
          }
          return null;
        },
        onSaved: (value) {
          _answers[field.key] = isNumber ? num.tryParse(value ?? '') : value;
        },
      ),
    );
  }

  Widget _dateField(FormFieldDef field) {
    DateTime? initialDateVal;
    if (_answers[field.key] != null) {
      initialDateVal = DateTime.tryParse(_answers[field.key].toString());
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: FormField<DateTime>(
        initialValue: initialDateVal,
        validator: (value) {
          if (field.required && value == null) return 'Ce champ est obligatoire';
          return null;
        },
        builder: (state) {
          return InkWell(
            onTap: () async {
              final picked = await showDatePicker(
                context: context,
                initialDate: state.value ?? DateTime.now(),
                firstDate: DateTime(2000),
                lastDate: DateTime(2100),
              );
              if (picked != null) {
                state.didChange(picked);
                _answers[field.key] = picked.toIso8601String();
              }
            },
            child: InputDecorator(
              decoration: InputDecoration(
                labelText: field.label + (field.required ? ' *' : ''),
                errorText: state.errorText,
              ),
              child: Text(
                state.value == null
                    ? 'Sélectionner une date'
                    : '${state.value!.day}/${state.value!.month}/${state.value!.year}',
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _selectField(FormFieldDef field) {
    final initialVal = _answers[field.key]?.toString();
    final isValidVal = (field.options ?? []).contains(initialVal) ? initialVal : null;

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: DropdownButtonFormField<String>(
        initialValue: isValidVal,
        decoration: InputDecoration(labelText: field.label + (field.required ? ' *' : '')),
        items: (field.options ?? [])
            .map((opt) => DropdownMenuItem(value: opt, child: Text(opt)))
            .toList(),
        validator: (value) {
          if (field.required && value == null) return 'Ce champ est obligatoire';
          return null;
        },
        onChanged: (val) => _answers[field.key] = val,
        onSaved: (value) => _answers[field.key] = value,
      ),
    );
  }

  Widget _photoField(FormFieldDef field) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: OutlinedButton.icon(
        icon: const Icon(Icons.add_a_photo_outlined),
        label: Text('${field.label} — photo'),
        onPressed: () async {
          final path = await _mediaService.takePhoto();
          if (path != null && mounted) {
            setState(() {
              _photoPaths.add(path);
              _answers[field.key] = path;
            });
          }
        },
      ),
    );
  }

  Widget _documentField(FormFieldDef field) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: OutlinedButton.icon(
        icon: const Icon(Icons.attach_file),
        label: Text('${field.label} — document'),
        onPressed: () async {
          final path = await _mediaService.pickDocument();
          if (path != null && mounted) {
            setState(() {
              _documentPaths.add(path);
              _answers[field.key] = path.split('/').last;
            });
          }
        },
      ),
    );
  }

  Widget _buildMissionSelector() {
    final missionsAsync = ref.watch(myMissionsProvider);

    return missionsAsync.when(
      loading: () => const SizedBox.shrink(),
      error: (error, stackTrace) => const SizedBox.shrink(),
      data: (List<MissionSummary> missions) {
        final activeMissions = missions.where((m) => m.status == 'ACTIVE').toList();

        if (activeMissions.isEmpty) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: Row(
              children: [
                Icon(Icons.info_outline, size: 16, color: Colors.grey.shade500),
                const SizedBox(width: 6),
                Text(
                  'Aucune mission active — collecte non rattachée',
                  style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                ),
              ],
            ),
          );
        }

        return Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: DropdownButtonFormField<int?>(
            initialValue: _selectedMissionId,
            decoration: const InputDecoration(
              labelText: 'Mission (optionnel)',
              prefixIcon: Icon(Icons.flag_outlined),
            ),
            items: [
              const DropdownMenuItem<int?>(
                value: null,
                child: Text('Aucune mission'),
              ),
              ...activeMissions.map(
                (m) => DropdownMenuItem<int?>(
                  value: m.id,
                  child: Text(m.name),
                ),
              ),
            ],
            onChanged: (value) {
              setState(() => _selectedMissionId = value);
            },
          ),
        );
      },
    );
  }
}
