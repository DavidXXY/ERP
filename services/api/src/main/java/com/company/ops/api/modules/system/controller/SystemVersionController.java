package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.version.ApplicationVersion;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemVersionController {

    private final ApplicationVersion applicationVersion;

    public SystemVersionController(ApplicationVersion applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @GetMapping("/version")
    public ApiResponse<Map<String, String>> getVersion() {
        return ApiResponse.ok(Map.of(
            "version", applicationVersion.getDisplayVersion(),
            "productVersion", applicationVersion.getProductVersion(),
            "commitId", applicationVersion.getCommitId(),
            "buildTime", applicationVersion.getBuildTime(),
            "appName", "工程运维一体化管理系统"
        ));
    }
}
