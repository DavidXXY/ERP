package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.domain.SystemWechatBinding;
import com.company.ops.api.modules.system.dto.LoginRequest;
import com.company.ops.api.modules.system.dto.LoginResponse;
import com.company.ops.api.modules.system.dto.WechatBindRequest;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.repository.SystemWechatBindingRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class WechatAuthService {
  private final boolean enabled;
  private final String appId;
  private final String appSecret;
  private final ObjectMapper objectMapper;
  private final SystemWechatBindingRepository bindingRepository;
  private final SystemUserRepository userRepository;
  private final AuthService authService;
  private final RestClient restClient;

  public WechatAuthService(
      @Value("${ops.wechat.enabled:false}") boolean enabled,
      @Value("${ops.wechat.app-id:}") String appId,
      @Value("${ops.wechat.app-secret:}") String appSecret,
      ObjectMapper objectMapper,
      SystemWechatBindingRepository bindingRepository,
      SystemUserRepository userRepository,
      AuthService authService
  ) {
    this.enabled = enabled;
    this.appId = appId;
    this.appSecret = appSecret;
    this.objectMapper = objectMapper;
    this.bindingRepository = bindingRepository;
    this.userRepository = userRepository;
    this.authService = authService;
    this.restClient = RestClient.builder().baseUrl("https://api.weixin.qq.com").build();
  }

  @Transactional
  public LoginResponse login(String code) {
    WechatIdentity identity = exchange(code);
    SystemWechatBinding binding = bindingRepository.findByAppIdAndOpenId(appId, identity.openId())
        .orElseThrow(() -> new BusinessException("微信账号未绑定ERP账号"));
    SystemUser user = userRepository.findDetailById(binding.getUserId())
        .filter(SystemUser::isEnabled)
        .orElseThrow(() -> new BusinessException("绑定的ERP账号不存在或已停用"));
    binding.setLastLoginAt(OffsetDateTime.now());
    bindingRepository.save(binding);
    return authService.issueSession(user);
  }

  @Transactional
  public LoginResponse bind(WechatBindRequest request, String clientAddress) {
    LoginResponse session = authService.login(new LoginRequest(request.username(), request.password()), clientAddress);
    SystemUser user = userRepository.findDetailById(UUID.fromString(session.user().id()))
        .orElseThrow(() -> new BusinessException("ERP账号不存在"));
    bindIdentity(user, exchange(request.code()));
    return session;
  }

  @Transactional
  public void bindCurrent(String code, UserPrincipal principal) {
    SystemUser user = userRepository.findDetailById(principal.id())
        .orElseThrow(() -> new BusinessException("ERP账号不存在"));
    bindIdentity(user, exchange(code));
  }

  private void bindIdentity(SystemUser user, WechatIdentity identity) {
    var openIdBinding = bindingRepository.findByAppIdAndOpenId(appId, identity.openId());
    if (openIdBinding.isPresent() && !openIdBinding.get().getUserId().equals(user.getId())) {
      throw new BusinessException("该微信已绑定其他ERP账号");
    }
    SystemWechatBinding binding = bindingRepository.findByAppIdAndUserId(appId, user.getId())
        .orElseGet(SystemWechatBinding::new);
    binding.setUserId(user.getId());
    binding.setAppId(appId);
    binding.setOpenId(identity.openId());
    binding.setUnionId(identity.unionId());
    binding.setLastLoginAt(OffsetDateTime.now());
    bindingRepository.save(binding);
  }

  private WechatIdentity exchange(String code) {
    if (!enabled || appId.isBlank() || appSecret.isBlank()) throw new BusinessException("微信登录尚未配置");
    try {
      String response = restClient.get().uri(builder -> builder.path("/sns/jscode2session")
          .queryParam("appid", appId)
          .queryParam("secret", appSecret)
          .queryParam("js_code", code)
          .queryParam("grant_type", "authorization_code")
          .build()).retrieve().body(String.class);
      JsonNode json = objectMapper.readTree(response);
      if (json.has("errcode") && json.path("errcode").asInt() != 0) {
        throw new BusinessException("微信登录凭证无效，请重新尝试");
      }
      String openId = json.path("openid").asText("");
      if (openId.isBlank()) throw new BusinessException("微信登录未返回用户标识");
      return new WechatIdentity(openId, json.path("unionid").asText(null));
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException("暂时无法连接微信登录服务");
    }
  }

  private record WechatIdentity(String openId, String unionId) {}
}
