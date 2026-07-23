package com.company.ops.api.modules.collaboration.repository;
import com.company.ops.api.modules.collaboration.domain.ResponsibilityCollaborator;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ResponsibilityCollaboratorRepository extends JpaRepository<ResponsibilityCollaborator,UUID>{
  List<ResponsibilityCollaborator> findByBindingId(UUID bindingId);
  List<ResponsibilityCollaborator> findByBindingIdIn(Collection<UUID> bindingIds);
  void deleteByBindingId(UUID bindingId);
}
