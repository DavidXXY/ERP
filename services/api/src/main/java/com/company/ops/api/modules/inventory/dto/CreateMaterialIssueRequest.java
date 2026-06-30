package com.company.ops.api.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateMaterialIssueRequest(
    @NotBlank @Size(max = 64) String code,
    @NotNull UUID projectId,
    @NotNull LocalDate issueDate,
    @NotBlank @Size(max = 80) String receiverName,
    @NotBlank @Size(max = 300) String purpose,
    @NotEmpty List<@Valid MaterialIssueLineRequest> lines
) {}
