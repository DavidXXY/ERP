package com.company.ops.api.modules.system.repository;

import com.company.ops.api.modules.system.domain.SystemWechatBinding;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemWechatBindingRepository extends JpaRepository<SystemWechatBinding, UUID> {
  Optional<SystemWechatBinding> findByAppIdAndOpenId(String appId, String openId);
  Optional<SystemWechatBinding> findByAppIdAndUserId(String appId, UUID userId);
}
