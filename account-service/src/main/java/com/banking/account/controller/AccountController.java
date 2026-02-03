package com.banking.account.controller;
import com.banking.account.dto.*;
import com.banking.account.entity.AccountStatus;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDTO account = accountService.createAccount(request);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }
    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<AccountDTO> debitAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(accountService.debitAccount(accountNumber, request));
    }
    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<AccountDTO> creditAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(accountService.creditAccount(accountNumber, request));
    }
    @PatchMapping("/{accountNumber}/status")
    public ResponseEntity<AccountDTO> updateAccountStatus(
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> body) {
        AccountStatus status = AccountStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, status));
    }
    @GetMapping("/{accountNumber}/history")
    public ResponseEntity<List<AccountHistoryDTO>> getAccountHistory(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountHistory(accountNumber));
    }
}
