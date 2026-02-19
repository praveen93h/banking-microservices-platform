package com.banking.transaction.repository;
import com.banking.transaction.entity.TransactionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface TransactionStepRepository extends JpaRepository<TransactionStep, Long> {
    List<TransactionStep> findByTransactionIdOrderByStepOrderAsc(String transactionId);
    List<TransactionStep> findByTransactionIdOrderByStepOrderDesc(String transactionId);
}
