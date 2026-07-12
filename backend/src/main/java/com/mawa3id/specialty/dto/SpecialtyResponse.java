package com.mawa3id.specialty.dto;

import com.mawa3id.specialty.Specialty;

public record SpecialtyResponse(Long id, String name, String description) {

    public static SpecialtyResponse from(Specialty specialty) {
        return new SpecialtyResponse(specialty.getId(), specialty.getName(), specialty.getDescription());
    }
}
