package com.company.ops.api.common.delete;

import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.common.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteGovernanceService {
  @PersistenceContext
  private EntityManager entityManager;

  public boolean allowPhysicalDelete(String entityType, UUID entityId, String title) {
    UserPrincipal principal = currentPrincipal();
    if (principal != null && principal.roleCodes().contains("ADMIN")) {
      return true;
    }
    createSoftDeleteRequest(entityType, entityId, title, principal);
    return false;
  }

  public boolean isHidden(String entityType, UUID entityId) {
    Number count = (Number) entityManager.createNativeQuery("""
        SELECT COUNT(*)
        FROM sys_soft_delete_records
        WHERE entity_type = ?1
          AND entity_id = ?2
          AND tenant_id = ?3
          AND status IN ('PENDING', 'APPROVED')
        """)
        .setParameter(1, entityType)
        .setParameter(2, entityId)
        .setParameter(3, TenantContext.currentTenant())
        .getSingleResult();
    return count.longValue() > 0;
  }

  public <T> List<T> visible(String entityType, List<T> rows, java.util.function.Function<T, UUID> idGetter) {
    if (rows.isEmpty()) return rows;
    Set<UUID> hiddenIds = hiddenIds(entityType);
    return rows.stream().filter(row -> !hiddenIds.contains(idGetter.apply(row))).toList();
  }

  public Set<UUID> hiddenIds(String entityType) {
    @SuppressWarnings("unchecked")
    List<UUID> hiddenRows = entityManager.createNativeQuery("""
        SELECT entity_id
        FROM sys_soft_delete_records
        WHERE entity_type = ?1
          AND tenant_id = ?2
          AND status IN ('PENDING', 'APPROVED')
        """, UUID.class)
        .setParameter(1, entityType)
        .setParameter(2, TenantContext.currentTenant())
        .getResultList();
    return new HashSet<>(hiddenRows);
  }

  @Transactional
  public void restore(UUID recordId) {
    requireAdmin();
    entityManager.createNativeQuery("""
        UPDATE sys_soft_delete_records
        SET status = 'RESTORED',
            restored_by = ?2,
            restored_at = ?3,
            updated_at = ?3
        WHERE id = ?1 AND tenant_id = ?4
        """)
        .setParameter(1, recordId)
        .setParameter(2, currentDisplayName())
        .setParameter(3, OffsetDateTime.now())
        .setParameter(4, TenantContext.currentTenant())
        .executeUpdate();
  }

  @Transactional
  public void approve(UUID recordId) {
    requireAdmin();
    entityManager.createNativeQuery("""
        UPDATE sys_soft_delete_records
        SET status = 'APPROVED',
            approved_by = ?2,
            approved_at = ?3,
            updated_at = ?3
        WHERE id = ?1 AND tenant_id = ?4 AND status = 'PENDING'
        """)
        .setParameter(1, recordId)
        .setParameter(2, currentDisplayName())
        .setParameter(3, OffsetDateTime.now())
        .setParameter(4, TenantContext.currentTenant())
        .executeUpdate();
  }

  @Transactional(readOnly = true)
  public List<DeletedRecordResponse> listActive() {
    requireAdmin();
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager.createNativeQuery("""
        SELECT id, entity_type, entity_id, title, status, requested_by, requested_at, approved_by, approved_at
        FROM sys_soft_delete_records
        WHERE tenant_id = ?1 AND status IN ('PENDING', 'APPROVED')
        ORDER BY requested_at DESC
        """).setParameter(1, TenantContext.currentTenant()).getResultList();
    return rows.stream().map(row -> new DeletedRecordResponse(
        (UUID) row[0],
        (String) row[1],
        (UUID) row[2],
        (String) row[3],
        (String) row[4],
        (String) row[5],
        (OffsetDateTime) row[6],
        (String) row[7],
        (OffsetDateTime) row[8]
    )).toList();
  }

  private void createSoftDeleteRequest(String entityType, UUID entityId, String title, UserPrincipal principal) {
    Number existing = (Number) entityManager.createNativeQuery("""
        SELECT COUNT(*)
        FROM sys_soft_delete_records
        WHERE entity_type = ?1
          AND entity_id = ?2
          AND tenant_id = ?3
          AND status IN ('PENDING', 'APPROVED')
        """)
        .setParameter(1, entityType)
        .setParameter(2, entityId)
        .setParameter(3, TenantContext.currentTenant())
        .getSingleResult();
    if (existing.longValue() > 0) {
      return;
    }

    UUID approvalId = UUID.randomUUID();
    UUID recordId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    String requester = principal == null ? "system" : principal.displayName();
    entityManager.createNativeQuery("""
        INSERT INTO oa_approval_requests (
          id, tenant_id, code, approval_type, title, source_no, status,
          applicant_name, content, business_type, created_at, updated_at
        ) VALUES (?1, ?8, ?2, 'OTHER', ?3, ?4, 'PENDING', ?5, ?6, 'DELETE', ?7, ?7)
        """)
        .setParameter(1, approvalId)
        .setParameter(2, "DEL-" + now.toInstant().toEpochMilli())
        .setParameter(3, "删除审批：" + title)
        .setParameter(4, entityType + ":" + entityId)
        .setParameter(5, requester)
        .setParameter(6, "非管理员删除申请，业务列表已隐藏。管理员可审批或恢复。")
        .setParameter(7, now)
        .setParameter(8, TenantContext.currentTenant())
        .executeUpdate();
    entityManager.createNativeQuery("""
        INSERT INTO sys_soft_delete_records (
          id, tenant_id, entity_type, entity_id, title, status,
          requested_by, requested_role_codes, requested_at, approval_id, created_at, updated_at
        ) VALUES (?1, ?9, ?2, ?3, ?4, 'PENDING', ?5, ?6, ?7, ?8, ?7, ?7)
        """)
        .setParameter(1, recordId)
        .setParameter(2, entityType)
        .setParameter(3, entityId)
        .setParameter(4, title)
        .setParameter(5, requester)
        .setParameter(6, principal == null ? "" : String.join(",", principal.roleCodes()))
        .setParameter(7, now)
        .setParameter(8, approvalId)
        .setParameter(9, TenantContext.currentTenant())
        .executeUpdate();
  }

  private void requireAdmin() {
    UserPrincipal principal = currentPrincipal();
    if (principal == null || !principal.roleCodes().contains("ADMIN")) {
      throw new org.springframework.security.access.AccessDeniedException("仅管理员可操作回收站");
    }
  }

  private String currentDisplayName() {
    UserPrincipal principal = currentPrincipal();
    return principal == null ? "system" : principal.displayName();
  }

  private UserPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
      return principal;
    }
    return null;
  }

  public record DeletedRecordResponse(
      UUID id,
      String entityType,
      UUID entityId,
      String title,
      String status,
      String requestedBy,
      OffsetDateTime requestedAt,
      String approvedBy,
      OffsetDateTime approvedAt
  ) {}
}
