package com.banking.user.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String eventType; // PROFILE_UPDATED, STATUS_CHANGED, KYC_UPDATED
    private LocalDateTime updatedAt;
}
