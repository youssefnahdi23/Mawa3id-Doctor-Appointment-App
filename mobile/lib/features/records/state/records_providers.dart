import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/record_models.dart';
import '../data/record_repository.dart';

/// The record for a single appointment (`null` when none exists yet).
/// Auto-disposes so a reopened screen always refetches the latest.
final appointmentRecordProvider = FutureProvider.autoDispose
    .family<MedicalRecordResponse?, int>((ref, appointmentId) =>
        ref.watch(recordRepositoryProvider).get(appointmentId));

class RecordListData {
  const RecordListData({
    required this.items,
    required this.isLast,
    required this.nextPage,
    this.loadingMore = false,
  });

  final List<MedicalRecordResponse> items;
  final bool isLast;
  final int nextPage;
  final bool loadingMore;

  RecordListData copyWith({
    List<MedicalRecordResponse>? items,
    bool? isLast,
    int? nextPage,
    bool? loadingMore,
  }) {
    return RecordListData(
      items: items ?? this.items,
      isLast: isLast ?? this.isLast,
      nextPage: nextPage ?? this.nextPage,
      loadingMore: loadingMore ?? this.loadingMore,
    );
  }
}

final patientRecordsProvider =
    AsyncNotifierProvider<PatientRecordsController, RecordListData>(
        PatientRecordsController.new);

/// Paged patient record history with infinite scroll, mirroring the
/// appointments controllers.
class PatientRecordsController extends AsyncNotifier<RecordListData> {
  @override
  Future<RecordListData> build() => _fetch(0);

  Future<RecordListData> _fetch(int page) async {
    final result = await ref.read(recordRepositoryProvider).mine(page: page);
    return RecordListData(
      items: result.content,
      isLast: result.isLast,
      nextPage: page + 1,
    );
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null || current.isLast || current.loadingMore) return;
    state = AsyncData(current.copyWith(loadingMore: true));
    try {
      final next = await _fetch(current.nextPage);
      state = AsyncData(RecordListData(
        items: [...current.items, ...next.items],
        isLast: next.isLast,
        nextPage: current.nextPage + 1,
      ));
    } catch (_) {
      state = AsyncData(current.copyWith(loadingMore: false));
    }
  }
}
