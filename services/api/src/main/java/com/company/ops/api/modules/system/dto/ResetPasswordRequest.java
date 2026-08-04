package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import com.company.ops.api.common.validation.StrongPassword;

public record ResetPasswordRequest(
    @NotBlank(message = "新密码不能为空")
    @StrongPassword
    String newPassword
) {}
