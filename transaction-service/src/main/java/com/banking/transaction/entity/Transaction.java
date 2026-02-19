package com.banking.transaction.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", unique = true, nullable = false, length = 50)
    private String transactionId;
    @Column(name = "from_account", nullable = false, length = 20)
    private String fromAccount;
    @Column(name = "to_account", nullable = false, length = 20)
    private String toAccount;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;
    @Column(name = "initiated_by")
    private Long initiatedBy;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
