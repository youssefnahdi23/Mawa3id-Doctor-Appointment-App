package com.mawa3id.patient;

import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.patient.dto.PatientUpdateRequest;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient samplePatient() {
        User user = new User("alice@example.com", "hash", Role.PATIENT);
        return new Patient(user, "Alice Doe", LocalDate.of(1990, 5, 20), "MW-AAAAAA");
    }

    @Test
    void generateUniquePatientCodeHasPrefixAndRetriesOnCollision() {
        // First candidate collides, second is free.
        when(patientRepository.existsByPatientCode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true, false);

        String code = patientService.generateUniquePatientCode();

        assertThat(code).startsWith("MW-");
        assertThat(code).hasSize(9); // "MW-" + 6 chars
        verify(patientRepository, times(2)).existsByPatientCode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getByUserIdThrowsWhenMissing() {
        when(patientRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getByUserId(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMutatesNameAndDateOfBirth() {
        Patient patient = samplePatient();
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));

        Patient result = patientService.update(1L,
                new PatientUpdateRequest("Alice Updated", LocalDate.of(1991, 1, 1)));

        assertThat(result.getFullName()).isEqualTo("Alice Updated");
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1991, 1, 1));
    }

    @Test
    void updateKeepsExistingDateOfBirthWhenNull() {
        Patient patient = samplePatient();
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));

        Patient result = patientService.update(1L,
                new PatientUpdateRequest("Alice Only Name", null));

        assertThat(result.getFullName()).isEqualTo("Alice Only Name");
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 20));
    }
}
