import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/empty_state.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/loading_skeleton.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/doctor_reference.dart';
import '../state/doctors_providers.dart';
import 'doctor_card.dart';

class DoctorListScreen extends ConsumerStatefulWidget {
  const DoctorListScreen({super.key});

  @override
  ConsumerState<DoctorListScreen> createState() => _DoctorListScreenState();
}

class _DoctorListScreenState extends ConsumerState<DoctorListScreen> {
  final _scroll = ScrollController();
  final _search = TextEditingController();
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300) {
        ref.read(doctorListControllerProvider.notifier).loadMore();
      }
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _scroll.dispose();
    _search.dispose();
    super.dispose();
  }

  void _onQueryChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      ref.read(doctorListControllerProvider.notifier).setQuery(value);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final languageCode = Localizations.localeOf(context).languageCode;
    final listState = ref.watch(doctorListControllerProvider);
    final controller = ref.read(doctorListControllerProvider.notifier);
    final specialties = ref.watch(specialtiesProvider);
    final specialtyById = {
      for (final s in specialties.valueOrNull ?? []) s.id: s,
    };

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabFind),
        actions: sessionAppBarActions(context, ref),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
                AppSpacing.lg, AppSpacing.sm, AppSpacing.lg, AppSpacing.sm),
            child: TextField(
              controller: _search,
              onChanged: _onQueryChanged,
              decoration: InputDecoration(
                hintText: l10n.searchDoctorsHint,
                prefixIcon: const Icon(Icons.search),
                isDense: true,
              ),
            ),
          ),
          // Specialty filter rail.
          SizedBox(
            height: 44,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              children: [
                FilterChip(
                  label: Text(l10n.allSpecialties),
                  selected: controller.specialtyId == null,
                  onSelected: (_) => controller.setSpecialty(null),
                ),
                for (final specialty in specialties.valueOrNull ?? [])
                  Padding(
                    padding: const EdgeInsetsDirectional.only(start: AppSpacing.sm),
                    child: FilterChip(
                      label: Text(specialty.localizedName(languageCode)),
                      selected: controller.specialtyId == specialty.id,
                      onSelected: (selected) =>
                          controller.setSpecialty(selected ? specialty.id : null),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          // CNAM + governorate ("distance") filters.
          Padding(
            padding:
                const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            child: Row(
              children: [
                FilterChip(
                  avatar: const Icon(Icons.verified_outlined, size: 18),
                  label: Text(l10n.cnamBadge),
                  selected: controller.cnamOnly,
                  onSelected: controller.setCnamOnly,
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String?>(
                      isExpanded: true,
                      value: controller.governorate,
                      icon: const Icon(Icons.expand_more),
                      hint: Text(l10n.allGovernorates),
                      items: [
                        DropdownMenuItem(
                            value: null, child: Text(l10n.allGovernorates)),
                        for (final code in kGovernorates)
                          DropdownMenuItem(
                              value: code,
                              child: Text(governorateLabel(l10n, code))),
                      ],
                      onChanged: controller.setGovernorate,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Expanded(
            child: listState.when(
              loading: () => const SkeletonList(),
              error: (error, _) => ErrorView(
                error: error,
                onRetry: () => ref.invalidate(doctorListControllerProvider),
              ),
              data: (data) {
                if (data.items.isEmpty) {
                  return EmptyState(
                    icon: Icons.search_off,
                    title: l10n.noDoctorsFound,
                  );
                }
                return RefreshIndicator(
                  onRefresh: () async =>
                      ref.invalidate(doctorListControllerProvider),
                  child: ListView.separated(
                    controller: _scroll,
                    padding: const EdgeInsets.fromLTRB(AppSpacing.lg, 0,
                        AppSpacing.lg, AppSpacing.xl),
                    itemCount: data.items.length + 1 + (data.isLast ? 0 : 1),
                    separatorBuilder: (_, __) =>
                        const SizedBox(height: AppSpacing.md),
                    itemBuilder: (context, index) {
                      if (index == 0) {
                        return Padding(
                          padding:
                              const EdgeInsets.only(bottom: AppSpacing.xs),
                          child: Text(
                            l10n.availableDoctors,
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
                                ?.copyWith(fontWeight: FontWeight.w700),
                          ),
                        );
                      }
                      final i = index - 1;
                      if (i >= data.items.length) {
                        return const Padding(
                          padding: EdgeInsets.all(AppSpacing.lg),
                          child: Center(child: CircularProgressIndicator()),
                        );
                      }
                      final doctor = data.items[i];
                      final specialtyLabel = doctor.specialtyId != null
                          ? specialtyById[doctor.specialtyId]
                                  ?.localizedName(languageCode) ??
                              doctor.specialtyName
                          : doctor.specialtyName;
                      return DoctorCard(
                        doctor: doctor,
                        specialtyLabel: specialtyLabel,
                        onTap: () => context.go('/p/doctors/${doctor.userId}'),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
