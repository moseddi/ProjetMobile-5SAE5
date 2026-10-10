package com.hangout.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GenerateDescriptionRequest(
        @NotBlank(message = "Le thème est obligatoire")
        @Size(max = 100, message = "Thème trop long (100 max)")
        String theme,

        @NotBlank(message = "La date est obligatoire")
        @Size(max = 40)
        String date,

        @NotBlank(message = "Le type de cuisine est obligatoire")
        @Size(max = 100, message = "Cuisine trop longue (100 max)")
        String cuisine,

        @Size(max = 200, message = "Menu trop long (200 max)")
        String menu,

        @Size(max = 100, message = "Ambiance trop longue (100 max)")
        String ambiance,

        @Positive(message = "Le nombre de places doit être positif")
        Integer places,

        @Size(max = 50)
        String style
) {}