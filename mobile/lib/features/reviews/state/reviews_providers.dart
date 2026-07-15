import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../doctors/data/doctor_models.dart' show ReviewResponse;
import '../data/review_repository.dart';

/// The patient's review for a single appointment (`null` when none exists
/// yet). Auto-disposes so a reopened form always reflects the latest.
final appointmentReviewProvider = FutureProvider.autoDispose
    .family<ReviewResponse?, int>((ref, appointmentId) =>
        ref.watch(reviewRepositoryProvider).get(appointmentId));
