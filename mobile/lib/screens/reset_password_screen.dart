import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/auth_provider.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';

class ResetPasswordScreen extends ConsumerStatefulWidget {
  final String token;
  const ResetPasswordScreen({super.key, required this.token});

  @override
  ConsumerState<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends ConsumerState<ResetPasswordScreen> {
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _isSubmitting = false;
  bool _success = false;
  String? _error;

  Future<void> _submit() async {
    if (widget.token.isEmpty) {
      setState(() => _error = "Lien de réinitialisation invalide ou incomplet (token manquant).");
      return;
    }
    if (_passwordController.text != _confirmController.text) {
      setState(() => _error = 'Les mots de passe ne correspondent pas');
      return;
    }
    if (_passwordController.text.length < 8) {
      setState(() => _error = 'Le mot de passe doit contenir au moins 8 caractères');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _error = null;
    });

    try {
      await ref.read(authServiceProvider).resetPassword(widget.token, _passwordController.text);
      if (!mounted) return;
      setState(() => _success = true);
    } on AuthException catch (e) {
      setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  void dispose() {
    _passwordController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Réinitialiser le mot de passe')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: _success ? _buildSuccessState() : _buildForm(),
      ),
    );
  }

  Widget _buildSuccessState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.check_circle, size: 56, color: Colors.green),
          const SizedBox(height: 16),
          const Text(
            'Mot de passe réinitialisé avec succès.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 24),
          ElevatedButton(
            onPressed: () => context.go('/login'),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary, foregroundColor: Colors.white),
            child: const Text('Se connecter'),
          ),
        ],
      ),
    );
  }

  Widget _buildForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Définissez votre nouveau mot de passe.',
          style: TextStyle(fontSize: 15),
        ),
        const SizedBox(height: 24),
        if (_error != null) ...[
          Text(_error!, style: const TextStyle(color: Colors.red)),
          const SizedBox(height: 16),
        ],
        const Text('Nouveau mot de passe'),
        const SizedBox(height: 6),
        TextField(controller: _passwordController, obscureText: true),
        const SizedBox(height: 16),
        const Text('Confirmer le mot de passe'),
        const SizedBox(height: 6),
        TextField(controller: _confirmController, obscureText: true),
        const SizedBox(height: 24),
        ElevatedButton(
          onPressed: _isSubmitting ? null : _submit,
          style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary, foregroundColor: Colors.white),
          child: _isSubmitting
              ? const SizedBox(
                  height: 20, width: 20,
                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                )
              : const Text('Réinitialiser mon mot de passe'),
        ),
      ],
    );
  }
}
