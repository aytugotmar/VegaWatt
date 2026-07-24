package com.vegawatt.core.user.api;

import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.user.application.ChangeEmailUseCase;
import com.vegawatt.core.user.application.ChangePasswordUseCase;
import com.vegawatt.core.user.application.ChangeUserRoleUseCase;
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
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ChangeEmailUseCase changeEmailUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;

    public UserController(UserRepository userRepository, ChangePasswordUseCase changePasswordUseCase,
                           ChangeEmailUseCase changeEmailUseCase, ChangeUserRoleUseCase changeUserRoleUseCase) {
        this.userRepository = userRepository;
        this.changePasswordUseCase = changePasswordUseCase;
        this.changeEmailUseCase = changeEmailUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Mevcut şifre zorunludur") String currentPassword,
            @NotBlank(message = "Yeni şifre zorunludur") @Size(min = 8, message = "Yeni şifre en az 8 karakter olmalıdır") String newPassword
    ) {}

    public record ChangeEmailRequest(
            @NotBlank(message = "Mevcut şifre zorunludur") String currentPassword,
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

        changePasswordUseCase.execute(currentUser.userId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(new OperationStatusResponse(true,
                "Şifreniz başarıyla değiştirildi. Güvenlik nedeniyle tüm oturumlarınız kapatıldı, lütfen tekrar giriş yapın."));
    }

    @PostMapping("/users/me/email")
    public ResponseEntity<OperationStatusResponse> changeEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangeEmailRequest request) {

        changeEmailUseCase.execute(currentUser.userId(), request.currentPassword(), request.newEmail());
        return ResponseEntity.ok(new OperationStatusResponse(true,
                "E-posta adresiniz başarıyla güncellendi. Güvenlik nedeniyle tüm oturumlarınız kapatıldı, lütfen tekrar giriş yapın."));
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

        changeUserRoleUseCase.execute(currentUser.userId(), targetUserId, request.role());
        return ResponseEntity.ok(new OperationStatusResponse(true, "Kullanıcı rolü başarıyla güncellendi."));
    }
}
