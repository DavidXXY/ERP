package com.company.ops.api.modules.ledger.repository;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountingVoucherRepository extends JpaRepository<AccountingVoucher,UUID>{List<AccountingVoucher> findAllByOrderByVoucherDateDescCreatedAtDesc(); boolean existsByBizTypeAndBizNo(String bizType,String bizNo); boolean existsByCode(String code);}
