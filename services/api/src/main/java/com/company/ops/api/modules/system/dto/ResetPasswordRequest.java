package com.company.ops.api.modules.system.dto;

import com.company.ops.api.common.validation.StrongPassword;

public record ResetPasswordRequest(
    @StrongPassword
    String newPassword
) {}
