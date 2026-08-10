package com.company.ops.api.modules.project.dto;

import jakarta.validation.constraints.Size;

public record CloseoutReviewRequest(
    @Size(max = 500) String comment
) {}
