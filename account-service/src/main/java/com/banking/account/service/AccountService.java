package com.banking.account.service;
import com.banking.account.client.UserServiceClient;
import com.banking.account.dto.*;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountHistory;
import com.banking.account.entity.AccountStatus;
import com.banking.account.event.AccountBalanceChangedEvent;
import com.banking.account.event.AccountCreatedEvent;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.exception.InvalidAccountOperationException;
import com.banking.account.repository.AccountHistoryRepository;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountHistoryRepository historyRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public AccountDTO createAccount(CreateAccountRequest request) {
        log.info("Creating account for user: {}", request.getUserId());
        UserDTO user = userServiceClient.getUserById(request.getUserId());
        if (user == null || !user.getIsActive()) {
            throw new InvalidAccountOperationException("User not found or inactive: " + request.getUserId());
        }
        String accountNumber = generateAccountNumber();
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .userId(request.getUserId())
                .accountType(request.getAccountType())
                .balance(request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(AccountStatus.ACTIVE)
                .minimumBalance(request.getMinimumBalance() != null ? request.getMinimumBalance() : BigDecimal.ZERO)
                .interestRate(getInterestRateByAccountType(request.getAccountType().name()))
                .build();
        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        recordHistory(savedAccount, "CREATED", null, savedAccount.getBalance(), null, null, "Account created");
        publishAccountCreatedEvent(savedAccount);
        return AccountDTO.from(savedAccount);
    }
    @Transactional(readOnly = true)
    public AccountDTO getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return AccountDTO.from(account);
    }
    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountDTO::from)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        BigDecimal availableBalance = account.getBalance().subtract(account.getMinimumBalance());
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO;
        }
        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .availableBalance(availableBalance)
                .build();
    }
    public AccountDTO debitAccount(String accountNumber, TransactionRequest request) {
        log.info("Debiting account {}: amount={}", accountNumber, request.getAmount());
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        validateAccountActive(account);
        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        if (newBalance.compareTo(account.getMinimumBalance()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %s, Required: %s, Minimum: %s",
                            account.getBalance(), request.getAmount(), account.getMinimumBalance()));
        }
        BigDecimal oldBalance = account.getBalance();
        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);
        recordHistory(updatedAccount, "DEBITED", oldBalance, newBalance, 
                request.getAmount(), request.getTransactionId(), request.getDescription());
        publishBalanceChangedEvent(updatedAccount, oldBalance, newBalance, 
                request.getAmount(), "DEBIT", request.getTransactionId());
        log.info("Account {} debited. New balance: {}", accountNumber, newBalance);
        return AccountDTO.from(updatedAccount);
    }
    public AccountDTO creditAccount(String accountNumber, TransactionRequest request) {
        log.info("Crediting account {}: amount={}", accountNumber, request.getAmount());
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        validateAccountActive(account);
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(request.getAmount());
        account.setBalance(newBalance);
        Account updatedAccount = accountRepository.save(account);
        recordHistory(updatedAccount, "CREDITED", oldBalance, newBalance, 
                request.getAmount(), request.getTransactionId(), request.getDescription());
        publishBalanceChangedEvent(updatedAccount, oldBalance, newBalance, 
                request.getAmount(), "CREDIT", request.getTransactionId());
        log.info("Account {} credited. New balance: {}", accountNumber, newBalance);
        return AccountDTO.from(updatedAccount);
    }
    public AccountDTO updateAccountStatus(String accountNumber, AccountStatus newStatus) {
        log.info("Updating account {} status to: {}", accountNumber, newStatus);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        String oldStatus = account.getStatus().name();
        account.setStatus(newStatus);
        Account updatedAccount = accountRepository.save(account);
        AccountHistory history = AccountHistory.builder()
                .accountId(updatedAccount.getId())
                .accountNumber(updatedAccount.getAccountNumber())
                .action("STATUS_CHANGED")
                .oldStatus(oldStatus)
                .newStatus(newStatus.name())
                .description("Account status changed from " + oldStatus + " to " + newStatus)
                .build();
        historyRepository.save(history);
        return AccountDTO.from(updatedAccount);
    }
    @Transactional(readOnly = true)
    public List<AccountHistoryDTO> getAccountHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return historyRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(AccountHistoryDTO::from)
                .collect(Collectors.toList());
    }
    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException(
                    "Account is not active. Status: " + account.getStatus());
        }
    }
    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "ACC" + System.currentTimeMillis() + 
                    String.format("%04d", new Random().nextInt(10000));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
    private BigDecimal getInterestRateByAccountType(String accountType) {
        return switch (accountType) {
            case "SAVINGS" -> new BigDecimal("3.50");
            case "FIXED_DEPOSIT" -> new BigDecimal("6.00");
            default -> BigDecimal.ZERO;
        };
    }
    private void recordHistory(Account account, String action, BigDecimal oldBalance, 
                               BigDecimal newBalance, BigDecimal amount, String transactionId, String description) {
        AccountHistory history = AccountHistory.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .action(action)
                .oldBalance(oldBalance)
                .newBalance(newBalance)
                .amount(amount)
                .transactionId(transactionId)
                .description(description)
                .build();
        historyRepository.save(history);
    }
    private void publishAccountCreatedEvent(Account account) {
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .accountType(account.getAccountType().name())
                .currency(account.getCurrency())
                .createdAt(LocalDateTime.now())
                .build();
        try {
            kafkaTemplate.send("account-events", event);
            log.info("Published AccountCreatedEvent for account: {}", account.getAccountNumber());
        } catch (Exception e) {
            log.error("Failed to publish AccountCreatedEvent: {}", e.getMessage());
        }
    }
    private void publishBalanceChangedEvent(Account account, BigDecimal oldBalance, 
                                            BigDecimal newBalance, BigDecimal amount, 
                                            String transactionType, String transactionId) {
        AccountBalanceChangedEvent event = AccountBalanceChangedEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .oldBalance(oldBalance)
                .newBalance(newBalance)
                .amount(amount)
                .transactionType(transactionType)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .build();
        try {
            kafkaTemplate.send("account-balance-events", event);
            log.info("Published AccountBalanceChangedEvent for account: {}", account.getAccountNumber());
        } catch (Exception e) {
            log.error("Failed to publish AccountBalanceChangedEvent: {}", e.getMessage());
        }
    }
}
