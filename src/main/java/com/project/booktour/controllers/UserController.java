package com.project.booktour.controllers;

import com.project.booktour.responses.usersresponse.LoginResponse;
import com.project.booktour.dtos.UpdateUserDTO;
import com.project.booktour.dtos.UserDTO;
import com.project.booktour.dtos.UserLoginDTO;
import com.project.booktour.exceptions.CustomException;
import com.project.booktour.models.User;
import com.project.booktour.responses.usersresponse.UserListResponse;
import com.project.booktour.responses.usersresponse.UserProfileResponse;
import com.project.booktour.responses.usersresponse.UserResponse;
import com.project.booktour.responses.ErrorResponse;
import com.project.booktour.services.user.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/users")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getAllUser(
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            PageRequest pageRequest = PageRequest.of(
                    page, limit,
                    Sort.by("id").ascending()
            );
            Page<UserResponse> userPage = userService.getAllUser(keyword, pageRequest)
                    .map(UserResponse::fromUser);
            int totalPages = userPage.getTotalPages();
            List<UserResponse> userResponses = userPage.getContent();
            return ResponseEntity.ok(UserListResponse
                    .builder()
                    .users(userResponses)
                    .totalPages(totalPages)
                    .build());
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #id == principal.userId")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        try {
            UserProfileResponse userProfile = userService.getUserProfile(id);
            return ResponseEntity.ok(userProfile);
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/avatars/{imageName}")
    public ResponseEntity<?> viewImage(@PathVariable String imageName) {
        try {
            Path imagePath = Paths.get("uploads/avatars/" + imageName);
            UrlResource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new UrlResource(Paths.get("https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png").toUri()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("IMAGE_NOT_FOUND", "Không tìm thấy ảnh đại diện: " + e.getMessage(), HttpStatus.NOT_FOUND.value()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody @Valid UserDTO userDTO, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream().map(FieldError::getDefaultMessage).toList();
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PARAM", String.join("; ", errorMessages), HttpStatus.BAD_REQUEST.value()));
            }
            if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PASSWORD", "Mật khẩu không khớp", HttpStatus.BAD_REQUEST.value()));
            }
            User user = userService.createUser(userDTO);
            return ResponseEntity.ok("Đăng ký người dùng thành công. Vui lòng kiểm tra email để kích hoạt tài khoản.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLoginDTO userLoginDTO, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream().map(FieldError::getDefaultMessage).toList();
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_PARAM", String.join("; ", errorMessages), HttpStatus.BAD_REQUEST.value()));
        }
        try {
            LoginResponse loginResponseDTO = userService.login(userLoginDTO.getUserName(), userLoginDTO.getPassword());
            return ResponseEntity.ok(loginResponseDTO);
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserDTO updatedUserDTO) {
        try {
            User updatedUser = userService.updateUser(id, updatedUserDTO);
            return ResponseEntity.ok("Cập nhật người dùng thành công.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{id}/avatar")
    public ResponseEntity<?> updateAvatar(@PathVariable Long id, @RequestParam("avatar") MultipartFile avatar) {
        try {
            User updatedUser = userService.updateAvatar(id, avatar);
            return ResponseEntity.ok(updatedUser);
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/block/{userId}/{active}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> blockOrEnable(@PathVariable long userId, @PathVariable int active) {
        try {
            userService.blockOrEnable(userId, active > 0);
            String message = active > 0 ? "Kích hoạt người dùng thành công." : "Chặn người dùng thành công.";
            return ResponseEntity.ok(message);
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok("Xóa người dùng thành công.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        try {
            userService.initiateResetPassword(email);
            return ResponseEntity.ok("Email đặt lại mật khẩu đã được gửi thành công.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword
    ) {
        try {
            userService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok("Đặt lại mật khẩu thành công.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(@RequestParam("token") String token) {
        try {
            userService.verifyResetToken(token);
            return ResponseEntity.ok("Token hợp lệ.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestParam("token") String token) {
        try {
            userService.activateAccount(token);
            return ResponseEntity.ok("Tài khoản đã được kích hoạt thành công.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivationEmail(@RequestParam("email") String email) {
        try {
            userService.resendActivationEmail(email);
            return ResponseEntity.ok("Email kích hoạt đã được gửi lại.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/activate-by-admin/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> activateAccountByAdmin(@PathVariable Long userId) {
        try {
            userService.activateAccountByAdmin(userId);
            return ResponseEntity.ok("Tài khoản đã được kích hoạt thành công bởi quản trị viên.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}