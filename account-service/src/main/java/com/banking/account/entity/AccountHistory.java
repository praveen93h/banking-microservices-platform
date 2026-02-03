package com.banking.account.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "account_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
    @Column(nullable = false, length = 50)
    private String action; // CREATED, BALANCE_UPDATED, STATUS_CHANGED, DEBITED, CREDITED
    @Column(name = "old_balance", precision = 15, scale = 2)
    private BigDecimal oldBalance;
    @Column(name = "new_balance", precision = 15, scale = 2)
    private BigDecimal newBalance;
    @Column(name = "old_status", length = 20)
    private String oldStatus;
    @Column(name = "new_status", length = 20)
    private String newStatus;
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "transaction_id")
    private String transactionId;
    @Column(name = "performed_by", length = 100)
    private String performedBy;
    private String description;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
