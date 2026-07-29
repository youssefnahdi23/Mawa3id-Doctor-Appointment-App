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
        when(doctorRepository.search(eq(5L), patternCaptor.capture(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        doctorService.search(5L, "   ", null, null, pageable);

        assertThat(patternCaptor.getValue()).isNull();
    }

    @Test
    void searchWithQueryTrimsLowercasesAndWrapsInLikeWildcards() {
        when(doctorRepository.search(any(), patternCaptor.capture(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        doctorService.search(null, "  SkIn  ", null, null, pageable);

        assertThat(patternCaptor.getValue()).isEqualTo("%skin%");
    }

    @Test
    void searchPassesCnamAndGovernorateFiltersThrough() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        // The stub only matches when cnam=true and governorate=SFAX are threaded through.
        when(doctorRepository.search(any(), any(), eq(Boolean.TRUE), eq(Governorate.SFAX), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doctor)));

        Page<Doctor> result = doctorService.search(null, null, true, Governorate.SFAX, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchReturnsRepositoryPage() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        Page<Doctor> page = new PageImpl<>(List.of(doctor));
        when(doctorRepository.search(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        assertThat(doctorService.search(null, null, null, null, pageable).getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateKeepsExistingSpecialtyWhenSpecialtyIdIsNull() {
        Specialty original = new Specialty("Cardiology", "Heart");
        Doctor doctor = sampleDoctor(original);
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));

        Doctor result = doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", null, "New Clinic", "8-16", "+212600", "bio", null, null,
                null, null, null, null));

        assertThat(result.getName()).isEqualTo("Dr After");
        assertThat(result.getSpecialty()).isSameAs(original);
        // acceptanceMode null -> unchanged from the entity default (MANUAL).
        assertThat(result.getAcceptanceMode()).isEqualTo(AcceptanceMode.MANUAL);
        // slotDurationMinutes null -> unchanged from the entity default (30).
        assertThat(result.getSlotDurationMinutes()).isEqualTo(30);
        // cnamConventionne null -> unchanged from the entity default (false).
        assertThat(result.isCnamConventionne()).isFalse();
    }

    @Test
    void updateAppliesNewSpecialtyAndAcceptanceModeWhenProvided() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));
        Specialty derma = new Specialty("Dermatology", "Skin");
        when(specialtyRepository.findById(9L)).thenReturn(Optional.of(derma));

        Doctor result = doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", 9L, "New Clinic", "8-16", "+212600", "bio", AcceptanceMode.AUTO, 45,
                true, Governorate.SFAX, 40, List.of("ar", "FR")));

        assertThat(result.getSpecialty()).isSameAs(derma);
        assertThat(result.getAcceptanceMode()).isEqualTo(AcceptanceMode.AUTO);
        assertThat(result.getSlotDurationMinutes()).isEqualTo(45);
        assertThat(result.isCnamConventionne()).isTrue();
        assertThat(result.getGovernorate()).isEqualTo(Governorate.SFAX);
        assertThat(result.getConsultationFee()).isEqualTo(40);
        // Languages are upper-cased, deduped, and joined for storage.
        assertThat(result.getLanguageList()).containsExactly("AR", "FR");
    }

    @Test
    void updateWithUnsupportedLanguageThrowsBadRequest() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", null, null, null, null, null, null, null,
                null, null, null, List.of("es"))))
                .isInstanceOf(com.mawa3id.common.ApiException.class);
    }

    @Test
    void updateClearsGovernorateAndFeeWhenNull() {
        Specialty original = new Specialty("Cardiology", "Heart");
        Doctor doctor = sampleDoctor(original);
        doctor.setGovernorate(Governorate.TUNIS);
        doctor.setConsultationFee(50);
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));

        Doctor result = doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", null, null, null, null, null, null, null,
                null, null, null, null));

        // governorate and fee are absolute (not skip-when-null) so blanks clear them.
        assertThat(result.getGovernorate()).isNull();
        assertThat(result.getConsultationFee()).isNull();
    }

    @Test
    void updateWithUnknownSpecialtyThrowsResourceNotFound() {
        Doctor doctor = sampleDoctor(new Specialty("Cardiology", "Heart"));
        when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));
        when(specialtyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.update(1L, new DoctorUpdateRequest(
                "Dr After", 999L, null, null, null, null, null, null,
                null, null, null, null)))
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
