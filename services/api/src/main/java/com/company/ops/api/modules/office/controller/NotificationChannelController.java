package com.company.ops.api.modules.office.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.office.domain.NotificationChannel;
import com.company.ops.api.modules.office.service.NotificationChannelService;
import com.company.ops.api.modules.office.service.NotificationChannelService.DeliveryResponse;
import com.company.ops.api.modules.office.service.NotificationChannelService.PreferenceResponse;
import com.company.ops.api.modules.system.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/office/notification-channels")
@PreAuthorize("isAuthenticated()")
public class NotificationChannelController {
  private final NotificationChannelService service;
  public NotificationChannelController(NotificationChannelService service) { this.service = service; }
  @GetMapping("/preferences")
  public ApiResponse<List<PreferenceResponse>> preferences(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.preferences(principal.id()));
  }
  @PutMapping("/preferences")
  public ApiResponse<PreferenceResponse> update(@AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody PreferenceRequest request) {
    return ApiResponse.ok(service.updatePreference(principal.id(), request.channel(), request.enabled()));
  }
  @GetMapping("/deliveries")
  public ApiResponse<List<DeliveryResponse>> deliveries(@AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(service.deliveries(principal.id()));
  }
  public record PreferenceRequest(@NotNull NotificationChannel channel, boolean enabled) {}
}
