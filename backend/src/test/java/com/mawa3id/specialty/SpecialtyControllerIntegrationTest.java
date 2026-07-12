package com.mawa3id.specialty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SpecialtyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long cardiologyId;

    @BeforeEach
    void seed() {
        // Insert out of alphabetical order to prove the endpoint sorts by name.
        specialtyRepository.save(new Specialty("Pediatrics", "Children"));
        cardiologyId = specialtyRepository.save(new Specialty("Cardiology", "Heart")).getId();
        specialtyRepository.save(new Specialty("Dermatology", "Skin"));
    }

    @Test
    void listIsPublicAndSortedByName() throws Exception {
        mockMvc.perform(get("/api/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Cardiology"))
                .andExpect(jsonPath("$[1].name").value("Dermatology"))
                .andExpect(jsonPath("$[2].name").value("Pediatrics"));
    }

    @Test
    void getByIdReturnsSpecialty() throws Exception {
        mockMvc.perform(get("/api/specialties/{id}", cardiologyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cardiology"))
                .andExpect(jsonPath("$.description").value("Heart"));
    }

    @Test
    void getByMissingIdReturns404() throws Exception {
        mockMvc.perform(get("/api/specialties/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
