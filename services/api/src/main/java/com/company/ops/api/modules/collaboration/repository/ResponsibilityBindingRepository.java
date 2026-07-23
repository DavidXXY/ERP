package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ResponsibilityBinding;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ResponsibilityBindingRepository extends JpaRepository<ResponsibilityBinding,UUID>{
  Optional<ResponsibilityBinding> findBySourceTypeAndSourceId(String sourceType,UUID sourceId);
}
