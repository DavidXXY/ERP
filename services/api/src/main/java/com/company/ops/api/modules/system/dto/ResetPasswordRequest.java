package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "新密码不能为空")
    String newPassword
) {}
