package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.service.CodeGenerator;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/code-rules")
public class CodeRuleController {
  private final CodeGenerator codeGenerator;

  public CodeRuleController(CodeGenerator codeGenerator) {
    this.codeGenerator = codeGenerator;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('system:role:view','system:permission:view')")
  public ApiResponse<Map<String, String>> list() {
    return ApiResponse.ok(codeGenerator.rules());
  }
}
