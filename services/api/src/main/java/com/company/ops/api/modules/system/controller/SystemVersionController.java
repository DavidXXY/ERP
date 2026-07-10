package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemVersionController {

    @Value("${ops.version:0.1.0}")
    private String version;

    @Value("${ops.build-time:}")
    private String buildTime;

    @GetMapping("/version")
    public ApiResponse<Map<String, String>> getVersion() {
        return ApiResponse.ok(Map.of(
            "version", version,
            "buildTime", buildTime,
            "appName", "工程运维一体化管理系统"
        ));
    }
}
