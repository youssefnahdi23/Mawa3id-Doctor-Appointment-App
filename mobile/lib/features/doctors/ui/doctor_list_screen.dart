import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/l10n.dart';
import '../../../core/widgets/empty_state.dart';
import '../../../core/widgets/error_view.dart';
import '../../../core/widgets/loading_skeleton.dart';
import '../../../core/widgets/rating_stars.dart';
import '../../../core/widgets/session_actions.dart';
import '../data/doctor_reference.dart';
import '../state/doctors_providers.dart';

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
      if (_scroll.position.pixels >
          _scroll.position.maxScrollExtent - 300) {
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
    // id -> specialty, so list rows can render the label in the active language.
    final specialtyById = {
      for (final s in specialties.valueOrNull ?? []) s.id: s,
    };

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.tabDoctors),
        actions: sessionAppBarActions(context, ref),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
            child: TextField(
              controller: _search,
              onChanged: _onQueryChanged,
              decoration: InputDecoration(
                hintText: l10n.searchDoctorsHint,
                prefixIcon: const Icon(Icons.search),
                border:
                    OutlineInputBorder(borderRadius: BorderRadius.circular(28)),
                isDense: true,
              ),
            ),
          ),
          SizedBox(
            height: 56,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              children: [
                FilterChip(
                  label: Text(l10n.allSpecialties),
                  selected: controller.specialtyId == null,
                  onSelected: (_) => controller.setSpecialty(null),
                ),
                for (final specialty in specialties.valueOrNull ?? [])
                  Padding(
                    padding: const EdgeInsetsDirectional.only(start: 8),
                    child: FilterChip(
                      label: Text(specialty.localizedName(languageCode)),
                      selected: controller.specialtyId == specialty.id,
                      onSelected: (selected) => controller
                          .setSpecialty(selected ? specialty.id : null),
                    ),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Row(
              children: [
                FilterChip(
                  label: Text(l10n.cnamBadge),
                  selected: controller.cnamOnly,
                  onSelected: controller.setCnamOnly,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: DropdownButton<String?>(
                    isExpanded: true,
                    value: controller.governorate,
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
              ],
            ),
          ),
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
                  child: ListView.builder(
                    controller: _scroll,
                    itemCount: data.items.length + (data.isLast ? 0 : 1),
                    itemBuilder: (context, index) {
                      if (index >= data.items.length) {
                        return const Padding(
                          padding: EdgeInsets.all(16),
                          child:
                              Center(child: CircularProgressIndicator()),
                        );
                      }
                      final doctor = data.items[index];
                      final specialtyLabel = doctor.specialtyId != null
                          ? specialtyById[doctor.specialtyId]
                                  ?.localizedName(languageCode) ??
                              doctor.specialtyName
                          : doctor.specialtyName;
                      return ListTile(
                        leading: CircleAvatar(
                            child: Text(doctor.name.isEmpty
                                ? '?'
                                : doctor.name[0].toUpperCase())),
                        title: Text(doctor.name),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (specialtyLabel != null) Text(specialtyLabel),
                            RatingStars(
                              average: doctor.ratingAverage,
                              count: doctor.ratingCount,
                            ),
                            _DoctorMetaRow(
                              cnam: doctor.cnamConventionne,
                              governorate: doctor.governorate,
                              consultationFee: doctor.consultationFee,
                            ),
                          ],
                        ),
                        isThreeLine: true,
                        onTap: () =>
                            context.go('/p/doctors/${doctor.userId}'),
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

/// Compact CNAM / governorate / fee strip shown under a doctor in the list.
class _DoctorMetaRow extends StatelessWidget {
  const _DoctorMetaRow({
    required this.cnam,
    required this.governorate,
    required this.consultationFee,
  });

  final bool cnam;
  final String? governorate;
  final int? consultationFee;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final theme = Theme.of(context);
    final govLabel = governorateLabel(l10n, governorate);
    final items = <Widget>[
      if (cnam)
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          decoration: BoxDecoration(
            color: theme.colorScheme.primaryContainer,
            borderRadius: BorderRadius.circular(6),
          ),
          child: Text(l10n.cnamBadge,
              style: theme.textTheme.labelSmall
                  ?.copyWith(color: theme.colorScheme.onPrimaryContainer)),
        ),
      if (govLabel.isNotEmpty)
        Text('📍 $govLabel', style: theme.textTheme.bodySmall),
      if (consultationFee != null)
        Text('$consultationFee ${l10n.consultationFeeUnit}',
            style: theme.textTheme.bodySmall),
    ];
    if (items.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Wrap(spacing: 8, runSpacing: 4, children: items),
    );
  }
}
