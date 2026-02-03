package com.banking.account.dto;
import com.banking.account.entity.AccountHistory;
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
public class AccountHistoryDTO {
    private Long id;
    private String accountNumber;
    private String action;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private BigDecimal amount;
    private String transactionId;
    private String description;
    private LocalDateTime createdAt;
    public static AccountHistoryDTO from(AccountHistory history) {
        return AccountHistoryDTO.builder()
                .id(history.getId())
                .accountNumber(history.getAccountNumber())
                .action(history.getAction())
                .oldBalance(history.getOldBalance())
                .newBalance(history.getNewBalance())
                .amount(history.getAmount())
                .transactionId(history.getTransactionId())
                .description(history.getDescription())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
