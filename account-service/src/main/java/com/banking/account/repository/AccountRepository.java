package com.banking.account.repository;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
    List<Account> findByUserId(Long userId);
    List<Account> findByStatus(AccountStatus status);
    boolean existsByAccountNumber(String accountNumber);
    @Query("SELECT COUNT(a) FROM Account a WHERE a.userId = :userId AND a.status = 'ACTIVE'")
    long countActiveAccountsByUserId(@Param("userId") Long userId);
}
