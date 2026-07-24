package com.company.ops.api.modules.project.dto;

import java.util.UUID;

public record ProjectManagerOption(
    UUID id,
    String username,
    String displayName
) {}
