package com.banking.transaction.controller;
import com.banking.transaction.dto.*;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionDTO transaction = transactionService.initiateTransfer(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }
    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionDTO transaction = transactionService.deposit(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@Valid @RequestBody WithdrawRequest request) {
        TransactionDTO transaction = transactionService.withdraw(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionDTO>> getTransactionsByAccount(
            @PathVariable String accountNumber,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountNumber, pageable));
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }
}
