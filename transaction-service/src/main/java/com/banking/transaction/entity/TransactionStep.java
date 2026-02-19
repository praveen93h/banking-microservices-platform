package com.banking.transaction.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name = "transaction_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", nullable = false, length = 50)
    private String transactionId;
    @Column(name = "step_name", nullable = false, length = 50)
    private String stepName; // DEBIT_FROM_ACCOUNT, CREDIT_TO_ACCOUNT
    @Column(name = "step_order")
    private Integer stepOrder;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
