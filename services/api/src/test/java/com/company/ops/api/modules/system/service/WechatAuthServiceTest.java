package com.company.ops.api.modules.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.repository.SystemWechatBindingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WechatAuthServiceTest {
  @Test
  void rejectsWechatLoginWhenCredentialsAreNotConfigured() {
    WechatAuthService service = new WechatAuthService(
        false, "", "", new ObjectMapper(),
        mock(SystemWechatBindingRepository.class), mock(SystemUserRepository.class), mock(AuthService.class)
    );

    assertThatThrownBy(() -> service.login("temporary-code"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("微信登录尚未配置");
  }
}
