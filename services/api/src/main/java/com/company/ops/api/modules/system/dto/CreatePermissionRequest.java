package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
    @NotBlank(message = "权限代码不能为空")
    String code,
    @NotBlank(message = "权限名称不能为空")
    String name,
    @NotBlank(message = "所属模块不能为空")
    String module
) {}
