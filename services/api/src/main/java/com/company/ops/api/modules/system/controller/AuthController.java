package com.company.ops.api.modules.system.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.system.dto.LoginRequest;
import com.company.ops.api.modules.system.dto.LoginResponse;
import com.company.ops.api.modules.system.dto.WechatLoginRequest;
import com.company.ops.api.modules.system.dto.WechatBindRequest;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.service.AuthService;
import com.company.ops.api.modules.system.service.WechatAuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final WechatAuthService wechatAuthService;

  public AuthController(AuthService authService, WechatAuthService wechatAuthService) {
    this.authService = authService;
    this.wechatAuthService = wechatAuthService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    return ApiResponse.ok(authService.login(request, servletRequest.getRemoteAddr()));
  }

  @PostMapping("/wechat/login")
  public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
    return ApiResponse.ok(wechatAuthService.login(request.code()));
  }

  @PostMapping("/wechat/bind")
  public ApiResponse<LoginResponse> bindWechat(@Valid @RequestBody WechatBindRequest request,
      HttpServletRequest servletRequest) {
    return ApiResponse.ok(wechatAuthService.bind(request, servletRequest.getRemoteAddr()));
  }

  @PostMapping("/wechat/bind-current")
  public ApiResponse<Void> bindCurrentWechat(@Valid @RequestBody WechatLoginRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    wechatAuthService.bindCurrent(request.code(), principal);
    return ApiResponse.ok();
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<LoginResponse.CurrentUserResponse>> currentUser(
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    var user = authService.currentUser(principal);
    if (user == null) {
      return ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body(new ApiResponse<>(false, "未登录", null));
    }
    return ResponseEntity.ok(ApiResponse.ok(user));
  }
}
