import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/appointments/ui/doctor_appointments_screen.dart';
import '../features/appointments/ui/patient_appointments_screen.dart';
import '../features/appointments/ui/slot_picker_screen.dart';
import '../features/auth/data/auth_models.dart';
import '../features/auth/state/session_controller.dart';
import '../features/auth/ui/forgot_password_screen.dart';
import '../features/auth/ui/login_screen.dart';
import '../features/auth/ui/register_doctor_screen.dart';
import '../features/auth/ui/register_patient_screen.dart';
import '../features/auth/ui/reset_password_screen.dart';
import '../features/auth/ui/verify_email_screen.dart';
import '../features/auth/ui/verify_phone_screen.dart';
import '../features/availability/ui/availability_editor_screen.dart';
import '../features/doctors/ui/doctor_detail_screen.dart';
import '../features/doctors/ui/doctor_list_screen.dart';
import '../features/doctors/ui/doctor_profile_screen.dart';
import '../features/notifications/ui/notifications_screen.dart';
import '../features/patient/ui/patient_profile_screen.dart';
import '../features/records/ui/patient_records_screen.dart';
import '../features/records/ui/record_detail_screen.dart';
import '../features/records/ui/record_form_screen.dart';
import '../features/reviews/ui/review_form_screen.dart';
import 'shells.dart';
import 'splash_screen.dart';

final routerProvider = Provider<GoRouter>((ref) {
  // go_router re-evaluates redirects whenever this notifier fires; bump it
  // on every session change (login, logout, boot restore finishing).
  final sessionChanged = ValueNotifier(0);
  ref.onDispose(sessionChanged.dispose);
  ref.listen(sessionControllerProvider, (_, __) => sessionChanged.value++);

  final router = GoRouter(
    initialLocation: '/splash',
    refreshListenable: sessionChanged,
    redirect: (context, state) {
      final session = ref.read(sessionControllerProvider);
      final location = state.matchedLocation;
      // Screens reachable while logged out: login, registration, and the
      // password-recovery flow.
      final onAuthScreen = location == '/login' ||
          location.startsWith('/register') ||
          location == '/forgot-password' ||
          location == '/reset-password';

      if (session.isLoading) return location == '/splash' ? null : '/splash';

      final user = session.valueOrNull;
      if (user == null) return onAuthScreen ? null : '/login';

      final home =
          user.role == UserRole.doctor ? '/d/appointments' : '/p/doctors';
      if (onAuthScreen || location == '/splash') return home;
      if (user.role == UserRole.patient && location.startsWith('/d')) {
        return home;
      }
      if (user.role == UserRole.doctor && location.startsWith('/p')) {
        return home;
      }
      return null;
    },
    routes: [
      GoRoute(path: '/splash', builder: (_, __) => const SplashScreen()),
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(
          path: '/register/patient',
          builder: (_, __) => const RegisterPatientScreen()),
      GoRoute(
          path: '/register/doctor',
          builder: (_, __) => const RegisterDoctorScreen()),
      GoRoute(
          path: '/forgot-password',
          builder: (_, __) => const ForgotPasswordScreen()),
      GoRoute(
          path: '/reset-password',
          builder: (_, __) => const ResetPasswordScreen()),
      GoRoute(
          path: '/verify/email',
          builder: (_, __) => const VerifyEmailScreen()),
      GoRoute(
          path: '/verify/phone',
          builder: (_, __) => const VerifyPhoneScreen()),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) =>
            RoleShell(navigationShell: shell, tabs: patientTabs),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/p/doctors',
              builder: (_, __) => const DoctorListScreen(),
              routes: [
                GoRoute(
                  path: ':id',
                  builder: (_, state) => DoctorDetailScreen(
                      doctorId: int.parse(state.pathParameters['id']!)),
                  routes: [
                    GoRoute(
                      path: 'book',
                      builder: (_, state) => SlotPickerScreen(
                          doctorId: int.parse(state.pathParameters['id']!)),
                    ),
                  ],
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/p/appointments',
              builder: (_, __) => const PatientAppointmentsScreen(),
              routes: [
                GoRoute(
                  path: 'records',
                  builder: (_, __) => const PatientRecordsScreen(),
                ),
                GoRoute(
                  path: ':id/record',
                  builder: (_, state) => RecordDetailScreen(
                      appointmentId: int.parse(state.pathParameters['id']!)),
                ),
                GoRoute(
                  path: ':id/review',
                  builder: (_, state) => ReviewFormScreen(
                    appointmentId: int.parse(state.pathParameters['id']!),
                    doctorId: int.parse(
                        state.uri.queryParameters['doctorId']!),
                  ),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: '/p/notifications',
                builder: (_, __) => const NotificationsScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: '/p/profile',
                builder: (_, __) => const PatientProfileScreen()),
          ]),
        ],
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) =>
            RoleShell(navigationShell: shell, tabs: doctorTabs),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(
              path: '/d/appointments',
              builder: (_, __) => const DoctorAppointmentsScreen(),
              routes: [
                GoRoute(
                  path: ':id/record',
                  builder: (_, state) => RecordFormScreen(
                      appointmentId: int.parse(state.pathParameters['id']!)),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: '/d/availability',
                builder: (_, __) => const AvailabilityEditorScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: '/d/notifications',
                builder: (_, __) => const NotificationsScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: '/d/profile',
                builder: (_, __) => const DoctorProfileScreen()),
          ]),
        ],
      ),
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});
