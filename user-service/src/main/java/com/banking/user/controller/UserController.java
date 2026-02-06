package com.banking.user.controller;
import com.banking.user.dto.UpdateUserRequest;
import com.banking.user.dto.UserDTO;
import com.banking.user.entity.KycStatus;
import com.banking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }
    @PatchMapping("/{userId}/kyc")
    public ResponseEntity<UserDTO> updateKycStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        KycStatus status = KycStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(userService.updateKycStatus(userId, status));
    }
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body) {
        boolean isActive = body.getOrDefault("isActive", true);
        return ResponseEntity.ok(userService.updateUserStatus(userId, isActive));
    }
}
