package com.project.booktour.services.user;

import com.project.booktour.responses.usersresponse.LoginResponse;
import com.project.booktour.dtos.UpdateUserDTO;
import com.project.booktour.dtos.UserDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.User;
import com.project.booktour.responses.usersresponse.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IUserService {
    // Tạo mới người dùng từ DTO
    User createUser(UserDTO userDTO) throws Exception;

    // Đăng nhập người dùng với tên đăng nhập và mật khẩu
    LoginResponse login(String userName, String password) throws Exception;

    // Cập nhật thông tin người dùng
    User updateUser(Long userId, UpdateUserDTO updatedUserDTO) throws Exception;

    // Cập nhật ảnh đại diện người dùng
    User updateAvatar(Long userId, MultipartFile avatar) throws Exception;

    // Lấy thông tin hồ sơ người dùng theo ID
    UserProfileResponse getUserProfile(Long userId) throws Exception;

    // Lấy danh sách người dùng với từ khóa tìm kiếm và phân trang
    Page<User> getAllUser(String keyword, Pageable pageable) throws Exception;

    // Khóa hoặc mở khóa người dùng
    void blockOrEnable(Long userId, Boolean active) throws DataNotFoundException;

    // Xóa người dùng theo ID
    void deleteUser(Long id) throws DataNotFoundException;

    // Khởi tạo quá trình đặt lại mật khẩu qua email
    void initiateResetPassword(String email) throws DataNotFoundException, IOException;

    // Đặt lại mật khẩu bằng mã token
    void resetPasswordWithToken(String token, String newPassword) throws DataNotFoundException;

    // Xác minh mã token đặt lại mật khẩu
    void verifyResetToken(String tokenStr) throws DataNotFoundException;

    // Kích hoạt tài khoản người dùng bằng mã token
    void activateAccount(String token) throws DataNotFoundException;

    // Gửi lại email kích hoạt tài khoản
    void resendActivationEmail(String email) throws DataNotFoundException, IOException;

    // Kích hoạt tài khoản bởi quản trị viên
    void activateAccountByAdmin(Long userId) throws DataNotFoundException;
}