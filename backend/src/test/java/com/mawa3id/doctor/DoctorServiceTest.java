package com.mawa3id.doctor;

import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.doctor.dto.DoctorUpdateRequest;
import com.mawa3id.specialty.Specialty;
import com.mawa3id.specialty.SpecialtyRepository;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoctorService} in isolation. These cover the branch logic
 * that the MockMvc integration tests do not exercise: the null-vs-pattern handling
 * in {@code search} and the partial-update (skip-when-null) semantics of {@code update}.
 */
@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private DoctorService doctorService;

    @Captor
    private ArgumentCaptor<String> patternCaptor;

    private final Pageable pageable = PageRequest.of(0, 20);

    private Doctor sampleDoctor(Specialty specialty) {
        User user = new User("doc@example.com", "hash", Role.DOCTOR);
        return new Doctor(user, "Dr Before", specialty, "Clinic", "9-17", "+212");
    }

    @Test
    void searchWithBlankQueryPassesNullNamePattern() {
        // Blank q -> null pattern (avoids LOWER(NULL) in SQL); the repository is the sink.
        when(doctorRepository.search(eq(5L), patternCaptor.capture(), any(Pageable.class)))
                .thenReturn(Page.empty());

        doctorService.search(5L, "   ", pageable);

        assertThat(patternCaptor.getValue()).isNull();
    }

    @Test
    void searchWithQueryTrimsLowercasesAndWrapsInLikeWildcards() {
        when(doctorRepository.search(any(), patternCaptor.capture(), any(Pageable.class)))
                .thenReturn(Page.empty());

        doctorService.search(null, "  SkIn  ", pageable);

        assertThat(patternCaptor.getValue()).isEqualTo("%skin%");
    }

    @Test
    void searchReturnsRepositoryPage() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        Page<Doctor> page = new PageImpl<>(List.of(doctor));
        when(doctorRepository.search(any(), any(), any(Pageable.class))).thenReturn(page);

        assertThat(doctorService.search(null, null, pageable).getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateKeepsExistingSpecialtyWhenSpecialtyIdIsNull() {
        Specialty original = new Specialty("Cardiology", "Heart");
        Doctor doctor = sampleDoctor(original);
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));

        Doctor result = doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", null, "New Clinic", "8-16", "+212600", "bio", null));

        assertThat(result.getName()).isEqualTo("Dr After");
        assertThat(result.getSpecialty()).isSameAs(original);
        // acceptanceMode null -> unchanged from the entity default (MANUAL).
        assertThat(result.getAcceptanceMode()).isEqualTo(AcceptanceMode.MANUAL);
    }

    @Test
    void updateAppliesNewSpecialtyAndAcceptanceModeWhenProvided() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));
        Specialty derma = new Specialty("Dermatology", "Skin");
        when(specialtyRepository.findById(9L)).thenReturn(Optional.of(derma));

        Doctor result = doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", 9L, "New Clinic", "8-16", "+212600", "bio", AcceptanceMode.AUTO));

        assertThat(result.getSpecialty()).isSameAs(derma);
        assertThat(result.getAcceptanceMode()).isEqualTo(AcceptanceMode.AUTO);
    }

    @Test
    void updateWithUnknownSpecialtyThrowsResourceNotFound() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));
        when(specialtyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", 999L, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByUserIdThrowsWhenMissing() {
        when(doctorRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.getByUserId(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveSpecialtyThrowsWhenMissing() {
        when(specialtyRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.resolveSpecialty(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
