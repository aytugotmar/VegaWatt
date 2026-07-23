package com.vegawatt.core.user.api;

import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.user.domain.EmailAlreadyRegisteredException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Mevcut şifre zorunludur") String currentPassword,
            @NotBlank(message = "Yeni şifre zorunludur") @Size(min = 8, message = "Yeni şifre en az 8 karakter olmalıdır") String newPassword
    ) {}

    public record ChangeEmailRequest(
            @NotBlank(message = "Yeni e-posta adresi zorunludur") @Email(message = "Geçerli bir e-posta adresi giriniz") String newEmail
    ) {}

    public record UpdateUserRoleRequest(
            @NotNull(message = "Rol zorunludur") UserRole role
    ) {}

    public record UserSummaryResponse(
            UUID id,
            String email,
            UserRole role,
            Instant createdAt
    ) {}

    public record OperationStatusResponse(
            boolean success,
            String message
    ) {}

    @PostMapping("/users/me/password")
    public ResponseEntity<OperationStatusResponse> changePassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            return ResponseEntity.badRequest().body(new OperationStatusResponse(false, "Mevcut şifreniz hatalı."));
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        User updated = user.changePassword(newHash);
        userRepository.save(updated);

        return ResponseEntity.ok(new OperationStatusResponse(true, "Şifreniz başarıyla değiştirildi."));
    }

    @PostMapping("/users/me/email")
    public ResponseEntity<OperationStatusResponse> changeEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangeEmailRequest request) {

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new EmailAlreadyRegisteredException(request.newEmail());
        }

        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        User updated = user.changeEmail(request.newEmail());
        userRepository.save(updated);

        return ResponseEntity.ok(new OperationStatusResponse(true, "E-posta adresiniz başarıyla güncellendi."));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers(
            @AuthenticationPrincipal CurrentUser currentUser) {

        List<UserSummaryResponse> users = userRepository.findAll().stream()
                .map(u -> new UserSummaryResponse(u.id(), u.email(), u.role(), u.createdAt()))
                .toList();

        return ResponseEntity.ok(users);
    }

    @PutMapping("/admin/users/{targetUserId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OperationStatusResponse> updateUserRole(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Hedef kullanıcı bulunamadı"));

        User updated = user.changeRole(request.role());
        userRepository.save(updated);

        return ResponseEntity.ok(new OperationStatusResponse(true, "Kullanıcı rolü başarıyla güncellendi."));
    }
}
