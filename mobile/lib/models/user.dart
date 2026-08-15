class AppUser {
  final int id;
  final String firstName;
  final String lastName;
  final String email;
  final String? phone;
  final String role;
  final String status;
  final int? organizationId;

  AppUser({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    this.phone,
    required this.role,
    required this.status,
    this.organizationId,
  });

  factory AppUser.fromJson(Map<String, dynamic> json) {
    return AppUser(
      id: json['id'] as int,
      firstName: json['firstName'] as String,
      lastName: json['lastName'] as String,
      email: json['email'] as String,
      phone: json['phone'] as String?,
      role: json['role'] as String,
      status: json['status'] as String,
      organizationId: json['organizationId'] as int?,
    );
  }

  String get fullName => '$firstName $lastName';
}