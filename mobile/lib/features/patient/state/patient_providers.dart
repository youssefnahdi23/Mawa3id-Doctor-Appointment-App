import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/patient_models.dart';
import '../data/patient_repository.dart';

/// The signed-in patient's own profile, for the profile editor.
final myPatientProfileProvider = FutureProvider.autoDispose<PatientResponse>(
    (ref) => ref.watch(patientRepositoryProvider).me());
