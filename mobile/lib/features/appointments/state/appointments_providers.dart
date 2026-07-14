import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/appointment_models.dart';
import '../data/appointment_repository.dart';
import '../logic/slot_utils.dart';

/// Bookable slots for the next 14 days, grouped by day. Auto-disposes so a
/// reopened picker always refetches (slots go stale fast).
final slotsProvider = FutureProvider.autoDispose
    .family<Map<DateTime, List<SlotResponse>>, int>((ref, doctorId) async {
  final now = DateTime.now();
  final from = DateTime(now.year, now.month, now.day);
  final to = DateTime(now.year, now.month, now.day + 13);
  final slots = await ref
      .watch(appointmentRepositoryProvider)
      .slots(doctorId, from: from, to: to);
  return groupSlotsByDay(slots);
});

class AppointmentListData {
  const AppointmentListData({
    required this.items,
    required this.isLast,
    required this.nextPage,
    this.loadingMore = false,
  });

  final List<AppointmentResponse> items;
  final bool isLast;
  final int nextPage;
  final bool loadingMore;

  AppointmentListData copyWith({
    List<AppointmentResponse>? items,
    bool? isLast,
    int? nextPage,
    bool? loadingMore,
  }) {
    return AppointmentListData(
      items: items ?? this.items,
      isLast: isLast ?? this.isLast,
      nextPage: nextPage ?? this.nextPage,
      loadingMore: loadingMore ?? this.loadingMore,
    );
  }
}

final patientAppointmentsProvider = AsyncNotifierProvider<
    PatientAppointmentsController,
    AppointmentListData>(PatientAppointmentsController.new);

final doctorAppointmentsProvider = AsyncNotifierProvider<
    DoctorAppointmentsController,
    AppointmentListData>(DoctorAppointmentsController.new);

/// Status-filtered, paged appointment list. Mutations (cancel/accept/...)
/// happen in the UI via the repository, then invalidate this provider.
abstract class AppointmentsController
    extends AsyncNotifier<AppointmentListData> {
  AppointmentStatus? _filter;

  AppointmentStatus? get filter => _filter;

  Future<PageResponseLike> fetchPage(int page);

  @override
  Future<AppointmentListData> build() => _fetch(0);

  Future<AppointmentListData> _fetch(int page) async {
    final result = await fetchPage(page);
    return AppointmentListData(
      items: result.items,
      isLast: result.isLast,
      nextPage: page + 1,
    );
  }

  Future<void> setFilter(AppointmentStatus? status) async {
    _filter = status;
    state = const AsyncLoading();
    state = await AsyncValue.guard(() => _fetch(0));
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null || current.isLast || current.loadingMore) return;
    state = AsyncData(current.copyWith(loadingMore: true));
    try {
      final next = await _fetch(current.nextPage);
      state = AsyncData(AppointmentListData(
        items: [...current.items, ...next.items],
        isLast: next.isLast,
        nextPage: current.nextPage + 1,
      ));
    } catch (_) {
      state = AsyncData(current.copyWith(loadingMore: false));
    }
  }
}

/// Minimal page shape the shared controller needs.
class PageResponseLike {
  const PageResponseLike({required this.items, required this.isLast});

  final List<AppointmentResponse> items;
  final bool isLast;
}

class PatientAppointmentsController extends AppointmentsController {
  @override
  Future<PageResponseLike> fetchPage(int page) async {
    final result = await ref
        .read(appointmentRepositoryProvider)
        .mine(status: filter, page: page);
    return PageResponseLike(items: result.content, isLast: result.isLast);
  }
}

class DoctorAppointmentsController extends AppointmentsController {
  @override
  Future<PageResponseLike> fetchPage(int page) async {
    final result = await ref
        .read(appointmentRepositoryProvider)
        .queue(status: filter, page: page);
    return PageResponseLike(items: result.content, isLast: result.isLast);
  }
}
