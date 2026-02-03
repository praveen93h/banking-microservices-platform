package com.banking.account.client;
import com.banking.account.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {
    @Override
    public UserDTO getUserById(Long userId) {
        log.warn("Fallback: User service unavailable for userId: {}", userId);
        return UserDTO.builder()
                .id(userId)
                .username("unknown")
                .email("unknown@unknown.com")
                .fullName("Unknown User")
                .isActive(false)
                .build();
    }
}
