import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/config/app_config.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/util/url_launcher_service.dart';
import '../../auth/state/session_controller.dart';

/// Community hub: send feedback, contribute code, and support the project.
/// Mawa3id is a community-built open-source project, so this gathers the ways
/// to get involved in one place.
class CommunityScreen extends ConsumerWidget {
  const CommunityScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    // Feedback + donate require a signed-in user; when reached from the login
    // screen's settings sheet, show only the (public) contribute section.
    final signedIn = ref.watch(sessionControllerProvider).valueOrNull != null;
    Future<void> open(Uri uri) => ref.read(urlLauncherProvider)(uri);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.communityTitle)),
      body: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: Text(l10n.communityIntro,
                style: Theme.of(context).textTheme.bodyMedium),
          ),
          if (signedIn) ...[
            _SectionHeader(l10n.feedbackSection),
            ListTile(
              leading: const Icon(Icons.feedback_outlined),
              title: Text(l10n.sendFeedback),
              subtitle: Text(l10n.sendFeedbackSubtitle),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => context.push('/community/feedback'),
            ),
          ],
          _SectionHeader(l10n.contributeSection),
          ListTile(
            leading: const Icon(Icons.code),
            title: Text(l10n.viewSourceOnGithub),
            trailing: const Icon(Icons.open_in_new, size: 18),
            onTap: () => open(AppConfig.repoUri),
          ),
          ListTile(
            leading: const Icon(Icons.menu_book_outlined),
            title: Text(l10n.contributingGuide),
            trailing: const Icon(Icons.open_in_new, size: 18),
            onTap: () => open(AppConfig.contributingUri),
          ),
          ListTile(
            leading: const Icon(Icons.bug_report_outlined),
            title: Text(l10n.openIssues),
            subtitle: Text(l10n.openIssuesSubtitle),
            trailing: const Icon(Icons.open_in_new, size: 18),
            onTap: () => open(AppConfig.issuesUri),
          ),
          if (signedIn) ...[
            _SectionHeader(l10n.supportSection),
            ListTile(
              leading: const Icon(Icons.volunteer_activism_outlined),
              title: Text(l10n.supportMawa3id),
              subtitle: Text(l10n.supportMawa3idSubtitle),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => context.push('/donate'),
            ),
          ],
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsetsDirectional.fromSTEB(16, 20, 16, 4),
      child: Text(
        label.toUpperCase(),
        style: theme.textTheme.labelMedium
            ?.copyWith(color: theme.colorScheme.primary),
      ),
    );
  }
}
