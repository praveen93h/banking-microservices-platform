package com.banking.account.repository;
import com.banking.account.entity.AccountHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistory, Long> {
    List<AccountHistory> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    Page<AccountHistory> findByAccountNumber(String accountNumber, Pageable pageable);
    List<AccountHistory> findByTransactionId(String transactionId);
}
