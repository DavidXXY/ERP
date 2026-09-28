package com.company.ops.api.modules.inventory.repository;

import com.company.ops.api.modules.inventory.domain.InventoryStocktake;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryStocktakeRepository extends JpaRepository<InventoryStocktake, UUID> {
  Page<InventoryStocktake> findAllByOrderByCreatedAtDesc(Pageable pageable);
  boolean existsByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from InventoryStocktake s where s.id = :id")
  Optional<InventoryStocktake> findByIdForUpdate(@Param("id") UUID id);
}
