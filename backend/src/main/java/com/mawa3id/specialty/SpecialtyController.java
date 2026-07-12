package com.mawa3id.specialty;

import com.mawa3id.common.ResourceNotFoundException;
import com.mawa3id.specialty.dto.SpecialtyResponse;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    public List<SpecialtyResponse> list() {
        return specialtyRepository.findAll(Sort.by("name")).stream()
                .map(SpecialtyResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SpecialtyResponse get(@PathVariable Long id) {
        return specialtyRepository.findById(id)
                .map(SpecialtyResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + id));
    }
}
