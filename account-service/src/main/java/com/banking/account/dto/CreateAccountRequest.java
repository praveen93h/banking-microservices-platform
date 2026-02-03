package com.banking.account.dto;
import com.banking.account.entity.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    @Builder.Default
    private String currency = "USD";
    @PositiveOrZero(message = "Initial deposit must be zero or positive")
    @Builder.Default
    private BigDecimal initialDeposit = BigDecimal.ZERO;
    @PositiveOrZero(message = "Minimum balance must be zero or positive")
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.ZERO;
}
