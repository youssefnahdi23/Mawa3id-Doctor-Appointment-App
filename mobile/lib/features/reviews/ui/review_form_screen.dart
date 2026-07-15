import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/error_view.dart';
import '../../doctors/data/doctor_models.dart' show ReviewResponse;
import '../../doctors/state/doctors_providers.dart';
import '../data/review_repository.dart';
import '../state/reviews_providers.dart';

/// Patient form to leave or edit a 1–5 star review for a completed visit.
class ReviewFormScreen extends ConsumerWidget {
  const ReviewFormScreen({
    super.key,
    required this.appointmentId,
    required this.doctorId,
  });

  final int appointmentId;
  final int doctorId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    final existing = ref.watch(appointmentReviewProvider(appointmentId));
    return Scaffold(
      appBar: AppBar(title: Text(l10n.reviewTitle)),
      body: existing.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () =>
              ref.invalidate(appointmentReviewProvider(appointmentId)),
        ),
        data: (review) => _ReviewForm(
          appointmentId: appointmentId,
          doctorId: doctorId,
          existing: review,
        ),
      ),
    );
  }
}

class _ReviewForm extends ConsumerStatefulWidget {
  const _ReviewForm({
    required this.appointmentId,
    required this.doctorId,
    required this.existing,
  });

  final int appointmentId;
  final int doctorId;
  final ReviewResponse? existing;

  @override
  ConsumerState<_ReviewForm> createState() => _ReviewFormState();
}

class _ReviewFormState extends ConsumerState<_ReviewForm> {
  late int _rating = widget.existing?.rating ?? 0;
  late final TextEditingController _comment =
      TextEditingController(text: widget.existing?.comment);
  bool _submitting = false;

  bool get _isEditing => widget.existing != null;

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_rating < 1) {
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(context.l10n.reviewRatingRequired)));
      return;
    }
    setState(() => _submitting = true);
    final repo = ref.read(reviewRepositoryProvider);
    final body = ReviewRequest(rating: _rating, comment: _comment.text.trim());
    try {
      if (_isEditing) {
        await repo.update(widget.appointmentId, body);
      } else {
        await repo.create(widget.appointmentId, body);
      }
      // Refresh this review plus the doctor's aggregate rating and review list.
      ref.invalidate(appointmentReviewProvider(widget.appointmentId));
      ref.invalidate(doctorDetailProvider(widget.doctorId));
      ref.invalidate(doctorReviewsProvider(widget.doctorId));
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(context.l10n.reviewSaved)));
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        setState(() => _submitting = false);
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(localizedErrorMessage(context, e))));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(l10n.reviewRating,
            style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        StarRatingInput(
          rating: _rating,
          onChanged: (value) => setState(() => _rating = value),
        ),
        const SizedBox(height: 24),
        TextField(
          controller: _comment,
          decoration: InputDecoration(
            labelText: l10n.reviewCommentOptional,
            border: const OutlineInputBorder(),
          ),
          maxLength: 2000,
          maxLines: 5,
        ),
        const SizedBox(height: 16),
        FilledButton(
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(strokeWidth: 2))
              : Text(_isEditing ? l10n.save : l10n.reviewSubmit),
        ),
      ],
    );
  }
}

/// Tappable 1–5 star selector — the interactive counterpart to the read-only
/// [RatingStars] display widget.
class StarRatingInput extends StatelessWidget {
  const StarRatingInput({
    super.key,
    required this.rating,
    required this.onChanged,
    this.size = 40,
  });

  final int rating;
  final ValueChanged<int> onChanged;
  final double size;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.tertiary;
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        for (var star = 1; star <= 5; star++)
          IconButton(
            iconSize: size,
            visualDensity: VisualDensity.compact,
            tooltip: '$star',
            icon: Icon(
              rating >= star ? Icons.star : Icons.star_border,
              color: color,
            ),
            onPressed: () => onChanged(star),
          ),
      ],
    );
  }
}
