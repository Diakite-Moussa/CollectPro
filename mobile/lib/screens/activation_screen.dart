import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/auth_provider.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';

enum _ActivationState { checking, invalid, ready }

class ActivationScreen extends ConsumerStatefulWidget {
  final String token;
  const ActivationScreen({super.key, required this.token});

  @override
  ConsumerState<ActivationScreen> createState() => _ActivationScreenState();
}

class _ActivationScreenState extends ConsumerState<ActivationScreen> {
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _isSubmitting = false;
  String? _submitError;

  _ActivationState _state = _ActivationState.checking;
  String? _invalidMessage;
  String? _firstName;
  String? _email;

  @override
  void initState() {
    super.initState();
    _checkToken();
  }

  Future<void> _checkToken() async {
    if (widget.token.isEmpty) {
      setState(() {
        _state = _ActivationState.invalid;
        _invalidMessage = "Lien d'activation invalide ou incomplet (token manquant).";
      });
      return;
    }
    try {
      final status = await ref.read(authServiceProvider).checkActivationToken(widget.token);
      if (!mounted) return;
      setState(() {
        _state = _ActivationState.ready;
        _firstName = status.firstName;
        _email = status.email;
      });
    } on AuthException catch (e) {
      if (!mounted) return;
      setState(() {
        _state = _ActivationState.invalid;
        _invalidMessage = e.message;
      });
    }
  }

  Future<void> _submit() async {
    if (_passwordController.text != _confirmController.text) {
      setState(() => _submitError = 'Les mots de passe ne correspondent pas');
      return;
    }
    if (_passwordController.text.length < 8) {
      setState(() => _submitError = 'Le mot de passe doit contenir au moins 8 caractères');
      return;
    }
    setState(() {
      _isSubmitting = true;
      _submitError = null;
    });

    try {
      await ref.read(authServiceProvider).activate(widget.token, _passwordController.text);
      if (!mounted) return;
      context.go('/login');
    } on AuthException catch (e) {
      setState(() => _submitError = e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Activer mon compte')),
      body: switch (_state) {
        _ActivationState.checking => const Center(child: CircularProgressIndicator()),
        _ActivationState.invalid => _buildInvalidState(),
        _ActivationState.ready => _buildForm(),
      },
    );
  }

  Widget _buildInvalidState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.link_off, size: 56, color: Colors.red),
            const SizedBox(height: 16),
            Text(
              _invalidMessage ?? "Ce lien d'activation n'est plus valide.",
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 15),
            ),
            const SizedBox(height: 24),
            OutlinedButton(
              onPressed: () => context.go('/login'),
              child: const Text('Retour à la connexion'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildForm() {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_firstName != null) ...[
            Text(
              'Bonjour $_firstName,',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            Text(
              'Définis un mot de passe pour activer ton compte${_email != null ? ' ($_email)' : ''}.',
              style: TextStyle(color: Colors.grey.shade600),
            ),
            const SizedBox(height: 24),
          ],
          if (_submitError != null) ...[
            Text(_submitError!, style: const TextStyle(color: Colors.red)),
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
                : const Text('Activer mon compte'),
          ),
        ],
      ),
    );
  }
}
