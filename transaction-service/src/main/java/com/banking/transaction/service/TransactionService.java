package com.banking.transaction.service;
import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.*;
import com.banking.transaction.entity.*;
import com.banking.transaction.event.TransactionEvent;
import com.banking.transaction.exception.TransactionException;
import com.banking.transaction.exception.TransactionNotFoundException;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.repository.TransactionStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionStepRepository stepRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Transactional
    public TransactionDTO initiateTransfer(TransferRequest request) {
        log.info("Initiating transfer from {} to {}, amount: {}", 
                request.getFromAccount(), request.getToAccount(), request.getAmount());
        validateAccounts(request.getFromAccount(), request.getToAccount());
        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .initiatedBy(request.getInitiatedBy())
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        createTransactionSteps(transactionId);
        executeSaga(savedTransaction);
        return TransactionDTO.from(transactionRepository.findByTransactionId(transactionId).orElseThrow());
    }
    @Transactional
    public TransactionDTO deposit(DepositRequest request) {
        log.info("Processing deposit to {}, amount: {}", request.getAccountNumber(), request.getAmount());
        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount("SYSTEM")
                .toAccount(request.getAccountNumber())
                .amount(request.getAmount())
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.PROCESSING)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .initiatedBy(request.getInitiatedBy())
                .build();
        transactionRepository.save(transaction);
        try {
            AccountTransactionRequest creditRequest = AccountTransactionRequest.builder()
                    .amount(request.getAmount())
                    .transactionId(transactionId)
                    .description("Deposit: " + request.getDescription())
                    .build();
            accountServiceClient.creditAccount(request.getAccountNumber(), creditRequest);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction, "COMPLETED");
            log.info("Deposit completed: {}", transactionId);
        } catch (Exception e) {
            log.error("Deposit failed: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction, "FAILED");
            throw new TransactionException("Deposit failed: " + e.getMessage());
        }
        return TransactionDTO.from(transaction);
    }
    @Transactional
    public TransactionDTO withdraw(WithdrawRequest request) {
        log.info("Processing withdrawal from {}, amount: {}", request.getAccountNumber(), request.getAmount());
        String transactionId = generateTransactionId();
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount(request.getAccountNumber())
                .toAccount("SYSTEM")
                .amount(request.getAmount())
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PROCESSING)
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
                .initiatedBy(request.getInitiatedBy())
                .build();
        transactionRepository.save(transaction);
        try {
            AccountTransactionRequest debitRequest = AccountTransactionRequest.builder()
                    .amount(request.getAmount())
                    .transactionId(transactionId)
                    .description("Withdrawal: " + request.getDescription())
                    .build();
            accountServiceClient.debitAccount(request.getAccountNumber(), debitRequest);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction, "COMPLETED");
            log.info("Withdrawal completed: {}", transactionId);
        } catch (Exception e) {
            log.error("Withdrawal failed: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction, "FAILED");
            throw new TransactionException("Withdrawal failed: " + e.getMessage());
        }
        return TransactionDTO.from(transaction);
    }
    @Transactional(readOnly = true)
    public TransactionDTO getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        return TransactionDTO.from(transaction);
    }
    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsByAccount(String accountNumber, Pageable pageable) {
        return transactionRepository.findByAccountNumber(accountNumber, pageable)
                .map(TransactionDTO::from);
    }
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByUser(Long userId) {
        return transactionRepository.findByInitiatedByOrderByCreatedAtDesc(userId).stream()
                .map(TransactionDTO::from)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status).stream()
                .map(TransactionDTO::from)
                .collect(Collectors.toList());
    }
    private void executeSaga(Transaction transaction) {
        log.info("Executing saga for transaction: {}", transaction.getTransactionId());
        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);
        List<TransactionStep> steps = stepRepository.findByTransactionIdOrderByStepOrderAsc(transaction.getTransactionId());
        try {
            TransactionStep debitStep = steps.get(0);
            executeDebitStep(transaction, debitStep);
            TransactionStep creditStep = steps.get(1);
            executeCreditStep(transaction, creditStep);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction, "COMPLETED");
            log.info("Transaction completed successfully: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Saga failed for transaction {}: {}", transaction.getTransactionId(), e.getMessage());
            compensateSaga(transaction, steps);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction, "FAILED");
        }
    }
    private void executeDebitStep(Transaction transaction, TransactionStep step) {
        log.info("Executing debit step for transaction: {}", transaction.getTransactionId());
        try {
            AccountTransactionRequest request = AccountTransactionRequest.builder()
                    .amount(transaction.getAmount())
                    .transactionId(transaction.getTransactionId())
                    .description("Transfer to " + transaction.getToAccount())
                    .build();
            accountServiceClient.debitAccount(transaction.getFromAccount(), request);
            step.setStatus(StepStatus.COMPLETED);
            step.setCompletedAt(LocalDateTime.now());
            stepRepository.save(step);
            log.info("Debit step completed for transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            step.setStatus(StepStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            stepRepository.save(step);
            throw new TransactionException("Debit failed: " + e.getMessage());
        }
    }
    private void executeCreditStep(Transaction transaction, TransactionStep step) {
        log.info("Executing credit step for transaction: {}", transaction.getTransactionId());
        try {
            AccountTransactionRequest request = AccountTransactionRequest.builder()
                    .amount(transaction.getAmount())
                    .transactionId(transaction.getTransactionId())
                    .description("Transfer from " + transaction.getFromAccount())
                    .build();
            accountServiceClient.creditAccount(transaction.getToAccount(), request);
            step.setStatus(StepStatus.COMPLETED);
            step.setCompletedAt(LocalDateTime.now());
            stepRepository.save(step);
            log.info("Credit step completed for transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            step.setStatus(StepStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            stepRepository.save(step);
            throw new TransactionException("Credit failed: " + e.getMessage());
        }
    }
    private void compensateSaga(Transaction transaction, List<TransactionStep> steps) {
        log.info("Executing compensation for transaction: {}", transaction.getTransactionId());
        for (int i = steps.size() - 1; i >= 0; i--) {
            TransactionStep step = steps.get(i);
            if (step.getStatus() == StepStatus.COMPLETED) {
                try {
                    compensateStep(transaction, step);
                    step.setStatus(StepStatus.COMPENSATED);
                    stepRepository.save(step);
                } catch (Exception e) {
                    log.error("Compensation failed for step {}: {}", step.getStepName(), e.getMessage());
                }
            }
        }
    }
    private void compensateStep(Transaction transaction, TransactionStep step) {
        if ("DEBIT_FROM_ACCOUNT".equals(step.getStepName())) {
            AccountTransactionRequest request = AccountTransactionRequest.builder()
                    .amount(transaction.getAmount())
                    .transactionId(transaction.getTransactionId() + "-REVERSAL")
                    .description("Reversal: " + transaction.getDescription())
                    .build();
            accountServiceClient.creditAccount(transaction.getFromAccount(), request);
            log.info("Compensated debit for transaction: {}", transaction.getTransactionId());
        }
    }
    private void validateAccounts(String fromAccount, String toAccount) {
        if (fromAccount.equals(toAccount)) {
            throw new TransactionException("Cannot transfer to the same account");
        }
        AccountDTO fromAcc = accountServiceClient.getAccount(fromAccount);
        if (fromAcc == null) {
            throw new TransactionException("Source account not found: " + fromAccount);
        }
        AccountDTO toAcc = accountServiceClient.getAccount(toAccount);
        if (toAcc == null) {
            throw new TransactionException("Destination account not found: " + toAccount);
        }
    }
    private void createTransactionSteps(String transactionId) {
        TransactionStep debitStep = TransactionStep.builder()
                .transactionId(transactionId)
                .stepName("DEBIT_FROM_ACCOUNT")
                .stepOrder(1)
                .status(StepStatus.PENDING)
                .build();
        TransactionStep creditStep = TransactionStep.builder()
                .transactionId(transactionId)
                .stepName("CREDIT_TO_ACCOUNT")
                .stepOrder(2)
                .status(StepStatus.PENDING)
                .build();
        stepRepository.save(debitStep);
        stepRepository.save(creditStep);
    }
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    private void publishTransactionEvent(Transaction transaction, String status) {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType().name())
                .status(status)
                .description(transaction.getDescription())
                .initiatedBy(transaction.getInitiatedBy())
                .timestamp(LocalDateTime.now())
                .build();
        String topic = "COMPLETED".equals(status) ? "transaction-completed" : "transaction-failed";
        try {
            kafkaTemplate.send(topic, event);
            log.info("Published transaction event to {}: {}", topic, transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish transaction event: {}", e.getMessage());
        }
    }
}
