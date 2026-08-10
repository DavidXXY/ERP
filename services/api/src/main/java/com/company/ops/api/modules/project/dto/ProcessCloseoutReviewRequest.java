package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.CloseoutReviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessCloseoutReviewRequest(
    @NotNull CloseoutReviewStatus decision,
    @NotBlank @Size(max = 500) String comment
) {}
