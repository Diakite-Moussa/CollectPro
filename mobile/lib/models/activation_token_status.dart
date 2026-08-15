class ActivationTokenStatus {
  final bool valid;
  final String? email;
  final String? firstName;
  final String? lastName;

  ActivationTokenStatus({
    required this.valid,
    this.email,
    this.firstName,
    this.lastName,
  });

  factory ActivationTokenStatus.fromJson(Map<String, dynamic> json) {
    return ActivationTokenStatus(
      valid: json['valid'] as bool? ?? false,
      email: json['email'] as String?,
      firstName: json['firstName'] as String?,
      lastName: json['lastName'] as String?,
    );
  }
}
