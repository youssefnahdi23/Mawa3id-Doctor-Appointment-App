import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../data/feedback_models.dart';
import '../data/feedback_repository.dart';

/// A short form for submitting app feedback: a category and a free-text message.
/// Posts to `POST /api/feedback` for the signed-in user.
class FeedbackScreen extends ConsumerStatefulWidget {
  const FeedbackScreen({super.key});

  @override
  ConsumerState<FeedbackScreen> createState() => _FeedbackScreenState();
}

class _FeedbackScreenState extends ConsumerState<FeedbackScreen> {
  final _formKey = GlobalKey<FormState>();
  final _message = TextEditingController();
  FeedbackCategory _category = FeedbackCategory.suggestion;
  bool _sending = false;
  String? _error;

  @override
  void dispose() {
    _message.dispose();
    super.dispose();
  }

  String _categoryLabel(AppLocalizations l10n, FeedbackCategory category) {
    switch (category) {
      case FeedbackCategory.bug:
        return l10n.feedbackCategoryBug;
      case FeedbackCategory.suggestion:
        return l10n.feedbackCategorySuggestion;
      case FeedbackCategory.praise:
        return l10n.feedbackCategoryPraise;
      case FeedbackCategory.other:
        return l10n.feedbackCategoryOther;
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _sending = true;
      _error = null;
    });
    try {
      await ref.read(feedbackRepositoryProvider).submit(
            category: _category,
            message: _message.text.trim(),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(context.l10n.feedbackSent)));
      Navigator.of(context).pop();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = localizedErrorMessage(context, e));
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.sendFeedback)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(l10n.feedbackIntro,
                      style: Theme.of(context).textTheme.bodyMedium),
                  const SizedBox(height: 16),
                  if (_error != null) ...[
                    _ErrorBanner(message: _error!),
                    const SizedBox(height: 8),
                  ],
                  DropdownButtonFormField<FeedbackCategory>(
                    initialValue: _category,
                    decoration:
                        InputDecoration(labelText: l10n.feedbackCategory),
                    items: [
                      for (final c in FeedbackCategory.values)
                        DropdownMenuItem(
                            value: c, child: Text(_categoryLabel(l10n, c))),
                    ],
                    onChanged: (v) =>
                        setState(() => _category = v ?? _category),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _message,
                    decoration: InputDecoration(
                      labelText: l10n.feedbackMessage,
                      alignLabelWithHint: true,
                    ),
                    minLines: 4,
                    maxLines: 8,
                    maxLength: 2000,
                    validator: (v) => v == null || v.trim().isEmpty
                        ? l10n.validationRequired
                        : null,
                  ),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: _sending ? null : _submit,
                    child: _sending
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : Text(l10n.feedbackSubmit),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Inline banner for whole-request failures (network, rate limit, etc.).
class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(message, style: TextStyle(color: colors.onErrorContainer)),
    );
  }
}
