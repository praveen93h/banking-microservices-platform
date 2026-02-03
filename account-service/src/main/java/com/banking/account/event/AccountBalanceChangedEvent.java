package com.banking.account.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceChangedEvent {
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private BigDecimal amount;
    private String transactionType; // DEBIT, CREDIT
    private String transactionId;
    private LocalDateTime timestamp;
}
