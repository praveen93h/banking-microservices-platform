package com.banking.transaction.client;
import com.banking.transaction.dto.AccountDTO;
import com.banking.transaction.dto.AccountTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class AccountServiceClientFallback implements AccountServiceClient {
    @Override
    public AccountDTO getAccount(String accountNumber) {
        log.warn("Fallback: Account service unavailable for account: {}", accountNumber);
        return null;
    }
    @Override
    public AccountDTO debitAccount(String accountNumber, AccountTransactionRequest request) {
        log.error("Fallback: Cannot debit account {}. Account service unavailable.", accountNumber);
        throw new RuntimeException("Account service unavailable for debit operation");
    }
    @Override
    public AccountDTO creditAccount(String accountNumber, AccountTransactionRequest request) {
        log.error("Fallback: Cannot credit account {}. Account service unavailable.", accountNumber);
        throw new RuntimeException("Account service unavailable for credit operation");
    }
}
