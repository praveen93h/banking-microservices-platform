package com.banking.transaction.client;
import com.banking.transaction.dto.AccountDTO;
import com.banking.transaction.dto.AccountTransactionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@FeignClient(name = "account-service", fallback = AccountServiceClientFallback.class)
public interface AccountServiceClient {
    @GetMapping("/api/accounts/{accountNumber}")
    AccountDTO getAccount(@PathVariable("accountNumber") String accountNumber);
    @PostMapping("/api/accounts/{accountNumber}/debit")
    AccountDTO debitAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountTransactionRequest request);
    @PostMapping("/api/accounts/{accountNumber}/credit")
    AccountDTO creditAccount(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountTransactionRequest request);
}
