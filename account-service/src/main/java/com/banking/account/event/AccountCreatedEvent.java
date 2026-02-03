package com.banking.account.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {
    private Long accountId;
    private String accountNumber;
    private Long userId;
    private String accountType;
    private String currency;
    private LocalDateTime createdAt;
}
