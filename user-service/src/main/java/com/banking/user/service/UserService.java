package com.banking.user.service;
import com.banking.user.dto.*;
import com.banking.user.entity.KycStatus;
import com.banking.user.entity.Role;
import com.banking.user.entity.RoleName;
import com.banking.user.entity.User;
import com.banking.user.event.UserCreatedEvent;
import com.banking.user.event.UserUpdatedEvent;
import com.banking.user.exception.ResourceNotFoundException;
import com.banking.user.exception.UserAlreadyExistsException;
import com.banking.user.repository.RoleRepository;
import com.banking.user.repository.UserRepository;
import com.banking.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public UserDTO registerUser(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.CUSTOMER);
                    return roleRepository.save(role);
                });
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .kycStatus(KycStatus.PENDING)
                .isActive(true)
                .roles(roles)
                .build();
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        publishUserCreatedEvent(savedUser);
        return UserDTO.from(savedUser);
    }
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Invalid credentials");
        }
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is inactive");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        log.info("Login successful for user: {}", user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserDTO.from(user);
    }
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserDTO.from(user);
    }
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }
    public UserDTO updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        User updatedUser = userRepository.save(user);
        publishUserUpdatedEvent(updatedUser, "PROFILE_UPDATED");
        return UserDTO.from(updatedUser);
    }
    public UserDTO updateKycStatus(Long userId, KycStatus kycStatus) {
        log.info("Updating KYC status for user {}: {}", userId, kycStatus);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setKycStatus(kycStatus);
        User updatedUser = userRepository.save(user);
        publishUserUpdatedEvent(updatedUser, "KYC_UPDATED");
        return UserDTO.from(updatedUser);
    }
    public UserDTO updateUserStatus(Long userId, boolean isActive) {
        log.info("Updating status for user {}: active={}", userId, isActive);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);
        publishUserUpdatedEvent(updatedUser, "STATUS_CHANGED");
        return UserDTO.from(updatedUser);
    }
    private void publishUserCreatedEvent(User user) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(LocalDateTime.now())
                .build();
        try {
            kafkaTemplate.send("user-events", event);
            log.info("Published UserCreatedEvent for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish UserCreatedEvent: {}", e.getMessage());
        }
    }
    private void publishUserUpdatedEvent(User user, String eventType) {
        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .eventType(eventType)
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            kafkaTemplate.send("user-updated", event);
            log.info("Published UserUpdatedEvent for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish UserUpdatedEvent: {}", e.getMessage());
        }
    }
}
