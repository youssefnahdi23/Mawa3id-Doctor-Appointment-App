import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/models/page_response.dart';
import '../../availability/data/availability_models.dart';
import '../../availability/data/availability_repository.dart';
import '../data/doctor_models.dart';
import '../data/doctor_repository.dart';

final specialtiesProvider = FutureProvider<List<SpecialtyResponse>>(
    (ref) => ref.watch(doctorRepositoryProvider).specialties());

/// The signed-in doctor's own profile, for the profile editor.
final myDoctorProfileProvider = FutureProvider.autoDispose<DoctorResponse>(
    (ref) => ref.watch(doctorRepositoryProvider).me());

final doctorDetailProvider = FutureProvider.family<DoctorResponse, int>(
    (ref, doctorId) => ref.watch(doctorRepositoryProvider).byId(doctorId));

/// A doctor's public weekly availability rules (incl. the RDV-only flag), used
/// to show patients the "Consultation hours" on the doctor detail screen.
final doctorAvailabilityProvider =
    FutureProvider.family<List<AvailabilityRule>, int>((ref, doctorId) =>
        ref.watch(availabilityRepositoryProvider).forDoctor(doctorId));

final doctorReviewsProvider =
    FutureProvider.family<PageResponse<ReviewResponse>, int>((ref, doctorId) =>
        ref.watch(doctorRepositoryProvider).reviews(doctorId, size: 10));

class DoctorListData {
  const DoctorListData({
    required this.items,
    required this.isLast,
    required this.nextPage,
    this.loadingMore = false,
  });

  final List<DoctorSummary> items;
  final bool isLast;
  final int nextPage;
  final bool loadingMore;

  DoctorListData copyWith({
    List<DoctorSummary>? items,
    bool? isLast,
    int? nextPage,
    bool? loadingMore,
  }) {
    return DoctorListData(
      items: items ?? this.items,
      isLast: isLast ?? this.isLast,
      nextPage: nextPage ?? this.nextPage,
      loadingMore: loadingMore ?? this.loadingMore,
    );
  }
}

final doctorListControllerProvider =
    AsyncNotifierProvider<DoctorListController, DoctorListData>(
        DoctorListController.new);

/// Search + filter + infinite scroll over `GET /api/doctors`.
class DoctorListController extends AsyncNotifier<DoctorListData> {
  String _query = '';
  int? _specialtyId;
  bool? _cnam;
  String? _governorate;

  String get query => _query;
  int? get specialtyId => _specialtyId;
  bool get cnamOnly => _cnam ?? false;
  String? get governorate => _governorate;

  @override
  Future<DoctorListData> build() => _fetchFirstPage();

  Future<DoctorListData> _fetchFirstPage() async {
    final page = await ref.read(doctorRepositoryProvider).list(
          specialtyId: _specialtyId,
          query: _query,
          cnam: _cnam,
          governorate: _governorate,
        );
    return DoctorListData(
      items: page.content,
      isLast: page.isLast,
      nextPage: page.page.number + 1,
    );
  }

  Future<void> setQuery(String query) {
    _query = query.trim();
    return _reload();
  }

  Future<void> setSpecialty(int? specialtyId) {
    _specialtyId = specialtyId;
    return _reload();
  }

  /// Toggles the "CNAM only" filter. Sends `cnam=true` when on, and no filter
  /// (both conventionné and non) when off.
  Future<void> setCnamOnly(bool value) {
    _cnam = value ? true : null;
    return _reload();
  }

  Future<void> setGovernorate(String? governorate) {
    _governorate = governorate;
    return _reload();
  }

  Future<void> _reload() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(_fetchFirstPage);
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null || current.isLast || current.loadingMore) return;
    state = AsyncData(current.copyWith(loadingMore: true));
    try {
      final page = await ref.read(doctorRepositoryProvider).list(
            specialtyId: _specialtyId,
            query: _query,
            cnam: _cnam,
            governorate: _governorate,
            page: current.nextPage,
          );
      state = AsyncData(DoctorListData(
        items: [...current.items, ...page.content],
        isLast: page.isLast,
        nextPage: page.page.number + 1,
      ));
    } catch (_) {
      // Keep what we have; the user can scroll again to retry.
      state = AsyncData(current.copyWith(loadingMore: false));
    }
  }
}
