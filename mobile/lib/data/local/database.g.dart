// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'database.dart';

// ignore_for_file: type=lint
class $CollectesTableTable extends CollectesTable
    with TableInfo<$CollectesTableTable, CollecteRow> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $CollectesTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _serverIdMeta = const VerificationMeta(
    'serverId',
  );
  @override
  late final GeneratedColumn<int> serverId = GeneratedColumn<int>(
    'server_id',
    aliasedName,
    true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _formVersionIdMeta = const VerificationMeta(
    'formVersionId',
  );
  @override
  late final GeneratedColumn<int> formVersionId = GeneratedColumn<int>(
    'form_version_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _missionIdMeta = const VerificationMeta(
    'missionId',
  );
  @override
  late final GeneratedColumn<int> missionId = GeneratedColumn<int>(
    'mission_id',
    aliasedName,
    true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _agentIdMeta = const VerificationMeta(
    'agentId',
  );
  @override
  late final GeneratedColumn<int> agentId = GeneratedColumn<int>(
    'agent_id',
    aliasedName,
    true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _dataJsonMeta = const VerificationMeta(
    'dataJson',
  );
  @override
  late final GeneratedColumn<String> dataJson = GeneratedColumn<String>(
    'data_json',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _latitudeMeta = const VerificationMeta(
    'latitude',
  );
  @override
  late final GeneratedColumn<double> latitude = GeneratedColumn<double>(
    'latitude',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _longitudeMeta = const VerificationMeta(
    'longitude',
  );
  @override
  late final GeneratedColumn<double> longitude = GeneratedColumn<double>(
    'longitude',
    aliasedName,
    true,
    type: DriftSqlType.double,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _photoPathsMeta = const VerificationMeta(
    'photoPaths',
  );
  @override
  late final GeneratedColumn<String> photoPaths = GeneratedColumn<String>(
    'photo_paths',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _documentPathsMeta = const VerificationMeta(
    'documentPaths',
  );
  @override
  late final GeneratedColumn<String> documentPaths = GeneratedColumn<String>(
    'document_paths',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _localStatusMeta = const VerificationMeta(
    'localStatus',
  );
  @override
  late final GeneratedColumn<String> localStatus = GeneratedColumn<String>(
    'local_status',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
    defaultValue: const Constant('DRAFT'),
  );
  static const VerificationMeta _serverStatusMeta = const VerificationMeta(
    'serverStatus',
  );
  @override
  late final GeneratedColumn<String> serverStatus = GeneratedColumn<String>(
    'server_status',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _validationCommentMeta = const VerificationMeta(
    'validationComment',
  );
  @override
  late final GeneratedColumn<String> validationComment =
      GeneratedColumn<String>(
        'validation_comment',
        aliasedName,
        true,
        type: DriftSqlType.string,
        requiredDuringInsert: false,
      );
  static const VerificationMeta _validatedByNameMeta = const VerificationMeta(
    'validatedByName',
  );
  @override
  late final GeneratedColumn<String> validatedByName = GeneratedColumn<String>(
    'validated_by_name',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _validatedAtMeta = const VerificationMeta(
    'validatedAt',
  );
  @override
  late final GeneratedColumn<DateTime> validatedAt = GeneratedColumn<DateTime>(
    'validated_at',
    aliasedName,
    true,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _syncErrorMessageMeta = const VerificationMeta(
    'syncErrorMessage',
  );
  @override
  late final GeneratedColumn<String> syncErrorMessage = GeneratedColumn<String>(
    'sync_error_message',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _createdAtMeta = const VerificationMeta(
    'createdAt',
  );
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
    'created_at',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
    defaultValue: currentDateAndTime,
  );
  static const VerificationMeta _updatedAtMeta = const VerificationMeta(
    'updatedAt',
  );
  @override
  late final GeneratedColumn<DateTime> updatedAt = GeneratedColumn<DateTime>(
    'updated_at',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
    defaultValue: currentDateAndTime,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    serverId,
    formVersionId,
    missionId,
    agentId,
    dataJson,
    latitude,
    longitude,
    photoPaths,
    documentPaths,
    localStatus,
    serverStatus,
    validationComment,
    validatedByName,
    validatedAt,
    syncErrorMessage,
    createdAt,
    updatedAt,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'collectes_table';
  @override
  VerificationContext validateIntegrity(
    Insertable<CollecteRow> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('server_id')) {
      context.handle(
        _serverIdMeta,
        serverId.isAcceptableOrUnknown(data['server_id']!, _serverIdMeta),
      );
    }
    if (data.containsKey('form_version_id')) {
      context.handle(
        _formVersionIdMeta,
        formVersionId.isAcceptableOrUnknown(
          data['form_version_id']!,
          _formVersionIdMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_formVersionIdMeta);
    }
    if (data.containsKey('mission_id')) {
      context.handle(
        _missionIdMeta,
        missionId.isAcceptableOrUnknown(data['mission_id']!, _missionIdMeta),
      );
    }
    if (data.containsKey('agent_id')) {
      context.handle(
        _agentIdMeta,
        agentId.isAcceptableOrUnknown(data['agent_id']!, _agentIdMeta),
      );
    }
    if (data.containsKey('data_json')) {
      context.handle(
        _dataJsonMeta,
        dataJson.isAcceptableOrUnknown(data['data_json']!, _dataJsonMeta),
      );
    } else if (isInserting) {
      context.missing(_dataJsonMeta);
    }
    if (data.containsKey('latitude')) {
      context.handle(
        _latitudeMeta,
        latitude.isAcceptableOrUnknown(data['latitude']!, _latitudeMeta),
      );
    }
    if (data.containsKey('longitude')) {
      context.handle(
        _longitudeMeta,
        longitude.isAcceptableOrUnknown(data['longitude']!, _longitudeMeta),
      );
    }
    if (data.containsKey('photo_paths')) {
      context.handle(
        _photoPathsMeta,
        photoPaths.isAcceptableOrUnknown(data['photo_paths']!, _photoPathsMeta),
      );
    }
    if (data.containsKey('document_paths')) {
      context.handle(
        _documentPathsMeta,
        documentPaths.isAcceptableOrUnknown(
          data['document_paths']!,
          _documentPathsMeta,
        ),
      );
    }
    if (data.containsKey('local_status')) {
      context.handle(
        _localStatusMeta,
        localStatus.isAcceptableOrUnknown(
          data['local_status']!,
          _localStatusMeta,
        ),
      );
    }
    if (data.containsKey('server_status')) {
      context.handle(
        _serverStatusMeta,
        serverStatus.isAcceptableOrUnknown(
          data['server_status']!,
          _serverStatusMeta,
        ),
      );
    }
    if (data.containsKey('validation_comment')) {
      context.handle(
        _validationCommentMeta,
        validationComment.isAcceptableOrUnknown(
          data['validation_comment']!,
          _validationCommentMeta,
        ),
      );
    }
    if (data.containsKey('validated_by_name')) {
      context.handle(
        _validatedByNameMeta,
        validatedByName.isAcceptableOrUnknown(
          data['validated_by_name']!,
          _validatedByNameMeta,
        ),
      );
    }
    if (data.containsKey('validated_at')) {
      context.handle(
        _validatedAtMeta,
        validatedAt.isAcceptableOrUnknown(
          data['validated_at']!,
          _validatedAtMeta,
        ),
      );
    }
    if (data.containsKey('sync_error_message')) {
      context.handle(
        _syncErrorMessageMeta,
        syncErrorMessage.isAcceptableOrUnknown(
          data['sync_error_message']!,
          _syncErrorMessageMeta,
        ),
      );
    }
    if (data.containsKey('created_at')) {
      context.handle(
        _createdAtMeta,
        createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta),
      );
    }
    if (data.containsKey('updated_at')) {
      context.handle(
        _updatedAtMeta,
        updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  CollecteRow map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return CollecteRow(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      serverId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}server_id'],
      ),
      formVersionId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}form_version_id'],
      )!,
      missionId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}mission_id'],
      ),
      agentId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}agent_id'],
      ),
      dataJson: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}data_json'],
      )!,
      latitude: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}latitude'],
      ),
      longitude: attachedDatabase.typeMapping.read(
        DriftSqlType.double,
        data['${effectivePrefix}longitude'],
      ),
      photoPaths: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}photo_paths'],
      ),
      documentPaths: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}document_paths'],
      ),
      localStatus: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}local_status'],
      )!,
      serverStatus: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}server_status'],
      ),
      validationComment: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}validation_comment'],
      ),
      validatedByName: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}validated_by_name'],
      ),
      validatedAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}validated_at'],
      ),
      syncErrorMessage: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}sync_error_message'],
      ),
      createdAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}created_at'],
      )!,
      updatedAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}updated_at'],
      )!,
    );
  }

  @override
  $CollectesTableTable createAlias(String alias) {
    return $CollectesTableTable(attachedDatabase, alias);
  }
}

class CollecteRow extends DataClass implements Insertable<CollecteRow> {
  final int id;
  final int? serverId;
  final int formVersionId;

  /// Mission active à laquelle rattacher cette collecte (optionnel).
  final int? missionId;

  /// Agent qui a saisi la collecte (User.id côté backend).
  /// Nullable pour compatibilité avec les lignes créées avant cette colonne.
  final int? agentId;
  final String dataJson;
  final double? latitude;
  final double? longitude;

  /// Chemins locaux des photos jointes, encodés en JSON (liste de String).
  /// Capture réelle non implémentée : nécessite le package image_picker.
  final String? photoPaths;

  /// Chemins locaux des documents joints, encodés en JSON (liste de String).
  final String? documentPaths;
  final String localStatus;

  /// Statut métier renvoyé par le serveur (PENDING_VALIDATION, VALIDATED,
  /// REJECTED...). Distinct de localStatus qui ne concerne que la synchro.
  final String? serverStatus;
  final String? validationComment;
  final String? validatedByName;
  final DateTime? validatedAt;
  final String? syncErrorMessage;
  final DateTime createdAt;
  final DateTime updatedAt;
  const CollecteRow({
    required this.id,
    this.serverId,
    required this.formVersionId,
    this.missionId,
    this.agentId,
    required this.dataJson,
    this.latitude,
    this.longitude,
    this.photoPaths,
    this.documentPaths,
    required this.localStatus,
    this.serverStatus,
    this.validationComment,
    this.validatedByName,
    this.validatedAt,
    this.syncErrorMessage,
    required this.createdAt,
    required this.updatedAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    if (!nullToAbsent || serverId != null) {
      map['server_id'] = Variable<int>(serverId);
    }
    map['form_version_id'] = Variable<int>(formVersionId);
    if (!nullToAbsent || missionId != null) {
      map['mission_id'] = Variable<int>(missionId);
    }
    if (!nullToAbsent || agentId != null) {
      map['agent_id'] = Variable<int>(agentId);
    }
    map['data_json'] = Variable<String>(dataJson);
    if (!nullToAbsent || latitude != null) {
      map['latitude'] = Variable<double>(latitude);
    }
    if (!nullToAbsent || longitude != null) {
      map['longitude'] = Variable<double>(longitude);
    }
    if (!nullToAbsent || photoPaths != null) {
      map['photo_paths'] = Variable<String>(photoPaths);
    }
    if (!nullToAbsent || documentPaths != null) {
      map['document_paths'] = Variable<String>(documentPaths);
    }
    map['local_status'] = Variable<String>(localStatus);
    if (!nullToAbsent || serverStatus != null) {
      map['server_status'] = Variable<String>(serverStatus);
    }
    if (!nullToAbsent || validationComment != null) {
      map['validation_comment'] = Variable<String>(validationComment);
    }
    if (!nullToAbsent || validatedByName != null) {
      map['validated_by_name'] = Variable<String>(validatedByName);
    }
    if (!nullToAbsent || validatedAt != null) {
      map['validated_at'] = Variable<DateTime>(validatedAt);
    }
    if (!nullToAbsent || syncErrorMessage != null) {
      map['sync_error_message'] = Variable<String>(syncErrorMessage);
    }
    map['created_at'] = Variable<DateTime>(createdAt);
    map['updated_at'] = Variable<DateTime>(updatedAt);
    return map;
  }

  CollectesTableCompanion toCompanion(bool nullToAbsent) {
    return CollectesTableCompanion(
      id: Value(id),
      serverId: serverId == null && nullToAbsent
          ? const Value.absent()
          : Value(serverId),
      formVersionId: Value(formVersionId),
      missionId: missionId == null && nullToAbsent
          ? const Value.absent()
          : Value(missionId),
      agentId: agentId == null && nullToAbsent
          ? const Value.absent()
          : Value(agentId),
      dataJson: Value(dataJson),
      latitude: latitude == null && nullToAbsent
          ? const Value.absent()
          : Value(latitude),
      longitude: longitude == null && nullToAbsent
          ? const Value.absent()
          : Value(longitude),
      photoPaths: photoPaths == null && nullToAbsent
          ? const Value.absent()
          : Value(photoPaths),
      documentPaths: documentPaths == null && nullToAbsent
          ? const Value.absent()
          : Value(documentPaths),
      localStatus: Value(localStatus),
      serverStatus: serverStatus == null && nullToAbsent
          ? const Value.absent()
          : Value(serverStatus),
      validationComment: validationComment == null && nullToAbsent
          ? const Value.absent()
          : Value(validationComment),
      validatedByName: validatedByName == null && nullToAbsent
          ? const Value.absent()
          : Value(validatedByName),
      validatedAt: validatedAt == null && nullToAbsent
          ? const Value.absent()
          : Value(validatedAt),
      syncErrorMessage: syncErrorMessage == null && nullToAbsent
          ? const Value.absent()
          : Value(syncErrorMessage),
      createdAt: Value(createdAt),
      updatedAt: Value(updatedAt),
    );
  }

  factory CollecteRow.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return CollecteRow(
      id: serializer.fromJson<int>(json['id']),
      serverId: serializer.fromJson<int?>(json['serverId']),
      formVersionId: serializer.fromJson<int>(json['formVersionId']),
      missionId: serializer.fromJson<int?>(json['missionId']),
      agentId: serializer.fromJson<int?>(json['agentId']),
      dataJson: serializer.fromJson<String>(json['dataJson']),
      latitude: serializer.fromJson<double?>(json['latitude']),
      longitude: serializer.fromJson<double?>(json['longitude']),
      photoPaths: serializer.fromJson<String?>(json['photoPaths']),
      documentPaths: serializer.fromJson<String?>(json['documentPaths']),
      localStatus: serializer.fromJson<String>(json['localStatus']),
      serverStatus: serializer.fromJson<String?>(json['serverStatus']),
      validationComment: serializer.fromJson<String?>(
        json['validationComment'],
      ),
      validatedByName: serializer.fromJson<String?>(json['validatedByName']),
      validatedAt: serializer.fromJson<DateTime?>(json['validatedAt']),
      syncErrorMessage: serializer.fromJson<String?>(json['syncErrorMessage']),
      createdAt: serializer.fromJson<DateTime>(json['createdAt']),
      updatedAt: serializer.fromJson<DateTime>(json['updatedAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'serverId': serializer.toJson<int?>(serverId),
      'formVersionId': serializer.toJson<int>(formVersionId),
      'missionId': serializer.toJson<int?>(missionId),
      'agentId': serializer.toJson<int?>(agentId),
      'dataJson': serializer.toJson<String>(dataJson),
      'latitude': serializer.toJson<double?>(latitude),
      'longitude': serializer.toJson<double?>(longitude),
      'photoPaths': serializer.toJson<String?>(photoPaths),
      'documentPaths': serializer.toJson<String?>(documentPaths),
      'localStatus': serializer.toJson<String>(localStatus),
      'serverStatus': serializer.toJson<String?>(serverStatus),
      'validationComment': serializer.toJson<String?>(validationComment),
      'validatedByName': serializer.toJson<String?>(validatedByName),
      'validatedAt': serializer.toJson<DateTime?>(validatedAt),
      'syncErrorMessage': serializer.toJson<String?>(syncErrorMessage),
      'createdAt': serializer.toJson<DateTime>(createdAt),
      'updatedAt': serializer.toJson<DateTime>(updatedAt),
    };
  }

  CollecteRow copyWith({
    int? id,
    Value<int?> serverId = const Value.absent(),
    int? formVersionId,
    Value<int?> missionId = const Value.absent(),
    Value<int?> agentId = const Value.absent(),
    String? dataJson,
    Value<double?> latitude = const Value.absent(),
    Value<double?> longitude = const Value.absent(),
    Value<String?> photoPaths = const Value.absent(),
    Value<String?> documentPaths = const Value.absent(),
    String? localStatus,
    Value<String?> serverStatus = const Value.absent(),
    Value<String?> validationComment = const Value.absent(),
    Value<String?> validatedByName = const Value.absent(),
    Value<DateTime?> validatedAt = const Value.absent(),
    Value<String?> syncErrorMessage = const Value.absent(),
    DateTime? createdAt,
    DateTime? updatedAt,
  }) => CollecteRow(
    id: id ?? this.id,
    serverId: serverId.present ? serverId.value : this.serverId,
    formVersionId: formVersionId ?? this.formVersionId,
    missionId: missionId.present ? missionId.value : this.missionId,
    agentId: agentId.present ? agentId.value : this.agentId,
    dataJson: dataJson ?? this.dataJson,
    latitude: latitude.present ? latitude.value : this.latitude,
    longitude: longitude.present ? longitude.value : this.longitude,
    photoPaths: photoPaths.present ? photoPaths.value : this.photoPaths,
    documentPaths: documentPaths.present
        ? documentPaths.value
        : this.documentPaths,
    localStatus: localStatus ?? this.localStatus,
    serverStatus: serverStatus.present ? serverStatus.value : this.serverStatus,
    validationComment: validationComment.present
        ? validationComment.value
        : this.validationComment,
    validatedByName: validatedByName.present
        ? validatedByName.value
        : this.validatedByName,
    validatedAt: validatedAt.present ? validatedAt.value : this.validatedAt,
    syncErrorMessage: syncErrorMessage.present
        ? syncErrorMessage.value
        : this.syncErrorMessage,
    createdAt: createdAt ?? this.createdAt,
    updatedAt: updatedAt ?? this.updatedAt,
  );
  CollecteRow copyWithCompanion(CollectesTableCompanion data) {
    return CollecteRow(
      id: data.id.present ? data.id.value : this.id,
      serverId: data.serverId.present ? data.serverId.value : this.serverId,
      formVersionId: data.formVersionId.present
          ? data.formVersionId.value
          : this.formVersionId,
      missionId: data.missionId.present ? data.missionId.value : this.missionId,
      agentId: data.agentId.present ? data.agentId.value : this.agentId,
      dataJson: data.dataJson.present ? data.dataJson.value : this.dataJson,
      latitude: data.latitude.present ? data.latitude.value : this.latitude,
      longitude: data.longitude.present ? data.longitude.value : this.longitude,
      photoPaths: data.photoPaths.present
          ? data.photoPaths.value
          : this.photoPaths,
      documentPaths: data.documentPaths.present
          ? data.documentPaths.value
          : this.documentPaths,
      localStatus: data.localStatus.present
          ? data.localStatus.value
          : this.localStatus,
      serverStatus: data.serverStatus.present
          ? data.serverStatus.value
          : this.serverStatus,
      validationComment: data.validationComment.present
          ? data.validationComment.value
          : this.validationComment,
      validatedByName: data.validatedByName.present
          ? data.validatedByName.value
          : this.validatedByName,
      validatedAt: data.validatedAt.present
          ? data.validatedAt.value
          : this.validatedAt,
      syncErrorMessage: data.syncErrorMessage.present
          ? data.syncErrorMessage.value
          : this.syncErrorMessage,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('CollecteRow(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('formVersionId: $formVersionId, ')
          ..write('missionId: $missionId, ')
          ..write('agentId: $agentId, ')
          ..write('dataJson: $dataJson, ')
          ..write('latitude: $latitude, ')
          ..write('longitude: $longitude, ')
          ..write('photoPaths: $photoPaths, ')
          ..write('documentPaths: $documentPaths, ')
          ..write('localStatus: $localStatus, ')
          ..write('serverStatus: $serverStatus, ')
          ..write('validationComment: $validationComment, ')
          ..write('validatedByName: $validatedByName, ')
          ..write('validatedAt: $validatedAt, ')
          ..write('syncErrorMessage: $syncErrorMessage, ')
          ..write('createdAt: $createdAt, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
    id,
    serverId,
    formVersionId,
    missionId,
    agentId,
    dataJson,
    latitude,
    longitude,
    photoPaths,
    documentPaths,
    localStatus,
    serverStatus,
    validationComment,
    validatedByName,
    validatedAt,
    syncErrorMessage,
    createdAt,
    updatedAt,
  );
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is CollecteRow &&
          other.id == this.id &&
          other.serverId == this.serverId &&
          other.formVersionId == this.formVersionId &&
          other.missionId == this.missionId &&
          other.agentId == this.agentId &&
          other.dataJson == this.dataJson &&
          other.latitude == this.latitude &&
          other.longitude == this.longitude &&
          other.photoPaths == this.photoPaths &&
          other.documentPaths == this.documentPaths &&
          other.localStatus == this.localStatus &&
          other.serverStatus == this.serverStatus &&
          other.validationComment == this.validationComment &&
          other.validatedByName == this.validatedByName &&
          other.validatedAt == this.validatedAt &&
          other.syncErrorMessage == this.syncErrorMessage &&
          other.createdAt == this.createdAt &&
          other.updatedAt == this.updatedAt);
}

class CollectesTableCompanion extends UpdateCompanion<CollecteRow> {
  final Value<int> id;
  final Value<int?> serverId;
  final Value<int> formVersionId;
  final Value<int?> missionId;
  final Value<int?> agentId;
  final Value<String> dataJson;
  final Value<double?> latitude;
  final Value<double?> longitude;
  final Value<String?> photoPaths;
  final Value<String?> documentPaths;
  final Value<String> localStatus;
  final Value<String?> serverStatus;
  final Value<String?> validationComment;
  final Value<String?> validatedByName;
  final Value<DateTime?> validatedAt;
  final Value<String?> syncErrorMessage;
  final Value<DateTime> createdAt;
  final Value<DateTime> updatedAt;
  const CollectesTableCompanion({
    this.id = const Value.absent(),
    this.serverId = const Value.absent(),
    this.formVersionId = const Value.absent(),
    this.missionId = const Value.absent(),
    this.agentId = const Value.absent(),
    this.dataJson = const Value.absent(),
    this.latitude = const Value.absent(),
    this.longitude = const Value.absent(),
    this.photoPaths = const Value.absent(),
    this.documentPaths = const Value.absent(),
    this.localStatus = const Value.absent(),
    this.serverStatus = const Value.absent(),
    this.validationComment = const Value.absent(),
    this.validatedByName = const Value.absent(),
    this.validatedAt = const Value.absent(),
    this.syncErrorMessage = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.updatedAt = const Value.absent(),
  });
  CollectesTableCompanion.insert({
    this.id = const Value.absent(),
    this.serverId = const Value.absent(),
    required int formVersionId,
    this.missionId = const Value.absent(),
    this.agentId = const Value.absent(),
    required String dataJson,
    this.latitude = const Value.absent(),
    this.longitude = const Value.absent(),
    this.photoPaths = const Value.absent(),
    this.documentPaths = const Value.absent(),
    this.localStatus = const Value.absent(),
    this.serverStatus = const Value.absent(),
    this.validationComment = const Value.absent(),
    this.validatedByName = const Value.absent(),
    this.validatedAt = const Value.absent(),
    this.syncErrorMessage = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.updatedAt = const Value.absent(),
  }) : formVersionId = Value(formVersionId),
       dataJson = Value(dataJson);
  static Insertable<CollecteRow> custom({
    Expression<int>? id,
    Expression<int>? serverId,
    Expression<int>? formVersionId,
    Expression<int>? missionId,
    Expression<int>? agentId,
    Expression<String>? dataJson,
    Expression<double>? latitude,
    Expression<double>? longitude,
    Expression<String>? photoPaths,
    Expression<String>? documentPaths,
    Expression<String>? localStatus,
    Expression<String>? serverStatus,
    Expression<String>? validationComment,
    Expression<String>? validatedByName,
    Expression<DateTime>? validatedAt,
    Expression<String>? syncErrorMessage,
    Expression<DateTime>? createdAt,
    Expression<DateTime>? updatedAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (serverId != null) 'server_id': serverId,
      if (formVersionId != null) 'form_version_id': formVersionId,
      if (missionId != null) 'mission_id': missionId,
      if (agentId != null) 'agent_id': agentId,
      if (dataJson != null) 'data_json': dataJson,
      if (latitude != null) 'latitude': latitude,
      if (longitude != null) 'longitude': longitude,
      if (photoPaths != null) 'photo_paths': photoPaths,
      if (documentPaths != null) 'document_paths': documentPaths,
      if (localStatus != null) 'local_status': localStatus,
      if (serverStatus != null) 'server_status': serverStatus,
      if (validationComment != null) 'validation_comment': validationComment,
      if (validatedByName != null) 'validated_by_name': validatedByName,
      if (validatedAt != null) 'validated_at': validatedAt,
      if (syncErrorMessage != null) 'sync_error_message': syncErrorMessage,
      if (createdAt != null) 'created_at': createdAt,
      if (updatedAt != null) 'updated_at': updatedAt,
    });
  }

  CollectesTableCompanion copyWith({
    Value<int>? id,
    Value<int?>? serverId,
    Value<int>? formVersionId,
    Value<int?>? missionId,
    Value<int?>? agentId,
    Value<String>? dataJson,
    Value<double?>? latitude,
    Value<double?>? longitude,
    Value<String?>? photoPaths,
    Value<String?>? documentPaths,
    Value<String>? localStatus,
    Value<String?>? serverStatus,
    Value<String?>? validationComment,
    Value<String?>? validatedByName,
    Value<DateTime?>? validatedAt,
    Value<String?>? syncErrorMessage,
    Value<DateTime>? createdAt,
    Value<DateTime>? updatedAt,
  }) {
    return CollectesTableCompanion(
      id: id ?? this.id,
      serverId: serverId ?? this.serverId,
      formVersionId: formVersionId ?? this.formVersionId,
      missionId: missionId ?? this.missionId,
      agentId: agentId ?? this.agentId,
      dataJson: dataJson ?? this.dataJson,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      photoPaths: photoPaths ?? this.photoPaths,
      documentPaths: documentPaths ?? this.documentPaths,
      localStatus: localStatus ?? this.localStatus,
      serverStatus: serverStatus ?? this.serverStatus,
      validationComment: validationComment ?? this.validationComment,
      validatedByName: validatedByName ?? this.validatedByName,
      validatedAt: validatedAt ?? this.validatedAt,
      syncErrorMessage: syncErrorMessage ?? this.syncErrorMessage,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (serverId.present) {
      map['server_id'] = Variable<int>(serverId.value);
    }
    if (formVersionId.present) {
      map['form_version_id'] = Variable<int>(formVersionId.value);
    }
    if (missionId.present) {
      map['mission_id'] = Variable<int>(missionId.value);
    }
    if (agentId.present) {
      map['agent_id'] = Variable<int>(agentId.value);
    }
    if (dataJson.present) {
      map['data_json'] = Variable<String>(dataJson.value);
    }
    if (latitude.present) {
      map['latitude'] = Variable<double>(latitude.value);
    }
    if (longitude.present) {
      map['longitude'] = Variable<double>(longitude.value);
    }
    if (photoPaths.present) {
      map['photo_paths'] = Variable<String>(photoPaths.value);
    }
    if (documentPaths.present) {
      map['document_paths'] = Variable<String>(documentPaths.value);
    }
    if (localStatus.present) {
      map['local_status'] = Variable<String>(localStatus.value);
    }
    if (serverStatus.present) {
      map['server_status'] = Variable<String>(serverStatus.value);
    }
    if (validationComment.present) {
      map['validation_comment'] = Variable<String>(validationComment.value);
    }
    if (validatedByName.present) {
      map['validated_by_name'] = Variable<String>(validatedByName.value);
    }
    if (validatedAt.present) {
      map['validated_at'] = Variable<DateTime>(validatedAt.value);
    }
    if (syncErrorMessage.present) {
      map['sync_error_message'] = Variable<String>(syncErrorMessage.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<DateTime>(updatedAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('CollectesTableCompanion(')
          ..write('id: $id, ')
          ..write('serverId: $serverId, ')
          ..write('formVersionId: $formVersionId, ')
          ..write('missionId: $missionId, ')
          ..write('agentId: $agentId, ')
          ..write('dataJson: $dataJson, ')
          ..write('latitude: $latitude, ')
          ..write('longitude: $longitude, ')
          ..write('photoPaths: $photoPaths, ')
          ..write('documentPaths: $documentPaths, ')
          ..write('localStatus: $localStatus, ')
          ..write('serverStatus: $serverStatus, ')
          ..write('validationComment: $validationComment, ')
          ..write('validatedByName: $validatedByName, ')
          ..write('validatedAt: $validatedAt, ')
          ..write('syncErrorMessage: $syncErrorMessage, ')
          ..write('createdAt: $createdAt, ')
          ..write('updatedAt: $updatedAt')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $CollectesTableTable collectesTable = $CollectesTableTable(this);
  late final CollecteDao collecteDao = CollecteDao(this as AppDatabase);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [collectesTable];
}

typedef $$CollectesTableTableCreateCompanionBuilder =
    CollectesTableCompanion Function({
      Value<int> id,
      Value<int?> serverId,
      required int formVersionId,
      Value<int?> missionId,
      Value<int?> agentId,
      required String dataJson,
      Value<double?> latitude,
      Value<double?> longitude,
      Value<String?> photoPaths,
      Value<String?> documentPaths,
      Value<String> localStatus,
      Value<String?> serverStatus,
      Value<String?> validationComment,
      Value<String?> validatedByName,
      Value<DateTime?> validatedAt,
      Value<String?> syncErrorMessage,
      Value<DateTime> createdAt,
      Value<DateTime> updatedAt,
    });
typedef $$CollectesTableTableUpdateCompanionBuilder =
    CollectesTableCompanion Function({
      Value<int> id,
      Value<int?> serverId,
      Value<int> formVersionId,
      Value<int?> missionId,
      Value<int?> agentId,
      Value<String> dataJson,
      Value<double?> latitude,
      Value<double?> longitude,
      Value<String?> photoPaths,
      Value<String?> documentPaths,
      Value<String> localStatus,
      Value<String?> serverStatus,
      Value<String?> validationComment,
      Value<String?> validatedByName,
      Value<DateTime?> validatedAt,
      Value<String?> syncErrorMessage,
      Value<DateTime> createdAt,
      Value<DateTime> updatedAt,
    });

class $$CollectesTableTableFilterComposer
    extends Composer<_$AppDatabase, $CollectesTableTable> {
  $$CollectesTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get serverId => $composableBuilder(
    column: $table.serverId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get formVersionId => $composableBuilder(
    column: $table.formVersionId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get missionId => $composableBuilder(
    column: $table.missionId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get agentId => $composableBuilder(
    column: $table.agentId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get dataJson => $composableBuilder(
    column: $table.dataJson,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get latitude => $composableBuilder(
    column: $table.latitude,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<double> get longitude => $composableBuilder(
    column: $table.longitude,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get photoPaths => $composableBuilder(
    column: $table.photoPaths,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get documentPaths => $composableBuilder(
    column: $table.documentPaths,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get localStatus => $composableBuilder(
    column: $table.localStatus,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get serverStatus => $composableBuilder(
    column: $table.serverStatus,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get validationComment => $composableBuilder(
    column: $table.validationComment,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get validatedByName => $composableBuilder(
    column: $table.validatedByName,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get validatedAt => $composableBuilder(
    column: $table.validatedAt,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get syncErrorMessage => $composableBuilder(
    column: $table.syncErrorMessage,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get updatedAt => $composableBuilder(
    column: $table.updatedAt,
    builder: (column) => ColumnFilters(column),
  );
}

class $$CollectesTableTableOrderingComposer
    extends Composer<_$AppDatabase, $CollectesTableTable> {
  $$CollectesTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get serverId => $composableBuilder(
    column: $table.serverId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get formVersionId => $composableBuilder(
    column: $table.formVersionId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get missionId => $composableBuilder(
    column: $table.missionId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get agentId => $composableBuilder(
    column: $table.agentId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get dataJson => $composableBuilder(
    column: $table.dataJson,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get latitude => $composableBuilder(
    column: $table.latitude,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<double> get longitude => $composableBuilder(
    column: $table.longitude,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get photoPaths => $composableBuilder(
    column: $table.photoPaths,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get documentPaths => $composableBuilder(
    column: $table.documentPaths,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get localStatus => $composableBuilder(
    column: $table.localStatus,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get serverStatus => $composableBuilder(
    column: $table.serverStatus,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get validationComment => $composableBuilder(
    column: $table.validationComment,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get validatedByName => $composableBuilder(
    column: $table.validatedByName,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get validatedAt => $composableBuilder(
    column: $table.validatedAt,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get syncErrorMessage => $composableBuilder(
    column: $table.syncErrorMessage,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get updatedAt => $composableBuilder(
    column: $table.updatedAt,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$CollectesTableTableAnnotationComposer
    extends Composer<_$AppDatabase, $CollectesTableTable> {
  $$CollectesTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<int> get serverId =>
      $composableBuilder(column: $table.serverId, builder: (column) => column);

  GeneratedColumn<int> get formVersionId => $composableBuilder(
    column: $table.formVersionId,
    builder: (column) => column,
  );

  GeneratedColumn<int> get missionId =>
      $composableBuilder(column: $table.missionId, builder: (column) => column);

  GeneratedColumn<int> get agentId =>
      $composableBuilder(column: $table.agentId, builder: (column) => column);

  GeneratedColumn<String> get dataJson =>
      $composableBuilder(column: $table.dataJson, builder: (column) => column);

  GeneratedColumn<double> get latitude =>
      $composableBuilder(column: $table.latitude, builder: (column) => column);

  GeneratedColumn<double> get longitude =>
      $composableBuilder(column: $table.longitude, builder: (column) => column);

  GeneratedColumn<String> get photoPaths => $composableBuilder(
    column: $table.photoPaths,
    builder: (column) => column,
  );

  GeneratedColumn<String> get documentPaths => $composableBuilder(
    column: $table.documentPaths,
    builder: (column) => column,
  );

  GeneratedColumn<String> get localStatus => $composableBuilder(
    column: $table.localStatus,
    builder: (column) => column,
  );

  GeneratedColumn<String> get serverStatus => $composableBuilder(
    column: $table.serverStatus,
    builder: (column) => column,
  );

  GeneratedColumn<String> get validationComment => $composableBuilder(
    column: $table.validationComment,
    builder: (column) => column,
  );

  GeneratedColumn<String> get validatedByName => $composableBuilder(
    column: $table.validatedByName,
    builder: (column) => column,
  );

  GeneratedColumn<DateTime> get validatedAt => $composableBuilder(
    column: $table.validatedAt,
    builder: (column) => column,
  );

  GeneratedColumn<String> get syncErrorMessage => $composableBuilder(
    column: $table.syncErrorMessage,
    builder: (column) => column,
  );

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);

  GeneratedColumn<DateTime> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);
}

class $$CollectesTableTableTableManager
    extends
        RootTableManager<
          _$AppDatabase,
          $CollectesTableTable,
          CollecteRow,
          $$CollectesTableTableFilterComposer,
          $$CollectesTableTableOrderingComposer,
          $$CollectesTableTableAnnotationComposer,
          $$CollectesTableTableCreateCompanionBuilder,
          $$CollectesTableTableUpdateCompanionBuilder,
          (
            CollecteRow,
            BaseReferences<_$AppDatabase, $CollectesTableTable, CollecteRow>,
          ),
          CollecteRow,
          PrefetchHooks Function()
        > {
  $$CollectesTableTableTableManager(
    _$AppDatabase db,
    $CollectesTableTable table,
  ) : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$CollectesTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$CollectesTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$CollectesTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<int?> serverId = const Value.absent(),
                Value<int> formVersionId = const Value.absent(),
                Value<int?> missionId = const Value.absent(),
                Value<int?> agentId = const Value.absent(),
                Value<String> dataJson = const Value.absent(),
                Value<double?> latitude = const Value.absent(),
                Value<double?> longitude = const Value.absent(),
                Value<String?> photoPaths = const Value.absent(),
                Value<String?> documentPaths = const Value.absent(),
                Value<String> localStatus = const Value.absent(),
                Value<String?> serverStatus = const Value.absent(),
                Value<String?> validationComment = const Value.absent(),
                Value<String?> validatedByName = const Value.absent(),
                Value<DateTime?> validatedAt = const Value.absent(),
                Value<String?> syncErrorMessage = const Value.absent(),
                Value<DateTime> createdAt = const Value.absent(),
                Value<DateTime> updatedAt = const Value.absent(),
              }) => CollectesTableCompanion(
                id: id,
                serverId: serverId,
                formVersionId: formVersionId,
                missionId: missionId,
                agentId: agentId,
                dataJson: dataJson,
                latitude: latitude,
                longitude: longitude,
                photoPaths: photoPaths,
                documentPaths: documentPaths,
                localStatus: localStatus,
                serverStatus: serverStatus,
                validationComment: validationComment,
                validatedByName: validatedByName,
                validatedAt: validatedAt,
                syncErrorMessage: syncErrorMessage,
                createdAt: createdAt,
                updatedAt: updatedAt,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<int?> serverId = const Value.absent(),
                required int formVersionId,
                Value<int?> missionId = const Value.absent(),
                Value<int?> agentId = const Value.absent(),
                required String dataJson,
                Value<double?> latitude = const Value.absent(),
                Value<double?> longitude = const Value.absent(),
                Value<String?> photoPaths = const Value.absent(),
                Value<String?> documentPaths = const Value.absent(),
                Value<String> localStatus = const Value.absent(),
                Value<String?> serverStatus = const Value.absent(),
                Value<String?> validationComment = const Value.absent(),
                Value<String?> validatedByName = const Value.absent(),
                Value<DateTime?> validatedAt = const Value.absent(),
                Value<String?> syncErrorMessage = const Value.absent(),
                Value<DateTime> createdAt = const Value.absent(),
                Value<DateTime> updatedAt = const Value.absent(),
              }) => CollectesTableCompanion.insert(
                id: id,
                serverId: serverId,
                formVersionId: formVersionId,
                missionId: missionId,
                agentId: agentId,
                dataJson: dataJson,
                latitude: latitude,
                longitude: longitude,
                photoPaths: photoPaths,
                documentPaths: documentPaths,
                localStatus: localStatus,
                serverStatus: serverStatus,
                validationComment: validationComment,
                validatedByName: validatedByName,
                validatedAt: validatedAt,
                syncErrorMessage: syncErrorMessage,
                createdAt: createdAt,
                updatedAt: updatedAt,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$CollectesTableTableProcessedTableManager =
    ProcessedTableManager<
      _$AppDatabase,
      $CollectesTableTable,
      CollecteRow,
      $$CollectesTableTableFilterComposer,
      $$CollectesTableTableOrderingComposer,
      $$CollectesTableTableAnnotationComposer,
      $$CollectesTableTableCreateCompanionBuilder,
      $$CollectesTableTableUpdateCompanionBuilder,
      (
        CollecteRow,
        BaseReferences<_$AppDatabase, $CollectesTableTable, CollecteRow>,
      ),
      CollecteRow,
      PrefetchHooks Function()
    >;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$CollectesTableTableTableManager get collectesTable =>
      $$CollectesTableTableTableManager(_db, _db.collectesTable);
}
