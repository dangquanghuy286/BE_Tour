package com.project.booktour.services.user;

import com.project.booktour.components.JwtTokenUtil;
import com.project.booktour.exceptions.AccountBlockedException;
import com.project.booktour.exceptions.AccountNotActivatedException;
import com.project.booktour.exceptions.InvalidPasswordException;
import com.project.booktour.responses.usersresponse.LoginResponse;
import com.project.booktour.dtos.UpdateUserDTO;
import com.project.booktour.dtos.UserDTO;
import com.project.booktour.models.Role;
import com.project.booktour.models.Token;
import com.project.booktour.models.User;
import com.project.booktour.repositories.RoleRepository;
import com.project.booktour.repositories.TokenRepository;
import com.project.booktour.repositories.UserRepository;
import com.project.booktour.responses.usersresponse.UserProfileResponse;
import com.project.booktour.responses.usersresponse.UserResponse;
import com.project.booktour.services.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.dao.DataIntegrityViolationException;
import com.project.booktour.exceptions.DataNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";
    private static final String DEFAULT_AVATAR_PATH = "/uploads/avatars/default-avatar.jpg";
    private static final String DEFAULT_AVATAR_URL = "https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png";

    @Override
    public User createUser(UserDTO userDTO) throws Exception {
        String userName = userDTO.getUserName();
        String email = userDTO.getEmail();

        if (userRepository.existsByUserName(userName)) {
            throw new DataIntegrityViolationException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DataIntegrityViolationException("Email đã tồn tại");
        }

        Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy vai trò"));

        User newUser = User.builder()
                .userName(userDTO.getUserName())
                .email(userDTO.getEmail())
                .fullName(userDTO.getFullName())
                .phoneNumber(userDTO.getPhoneNumber())
                .address(userDTO.getAddress())
                .dateOfBirth(userDTO.getDateOfBirth())
                .password(userDTO.getPassword())
                .facebookAccountId(userDTO.getFacebookAccountId() != null ? userDTO.getFacebookAccountId() : 0)
                .googleAccountId(userDTO.getGoogleAccountId() != null ? userDTO.getGoogleAccountId() : 0)
                .role(role)
                .isActive(true)
                .isActivated(false)
                .avatar(DEFAULT_AVATAR_PATH)
                .build();

        if (newUser.getFacebookAccountId() == 0 && newUser.getGoogleAccountId() == 0) {
            String password = userDTO.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            newUser.setPassword(encodedPassword);
        }

        String activationToken = UUID.randomUUID().toString();
        LocalDateTime expiration = LocalDateTime.now().plusHours(24);
        newUser.setActivationToken(activationToken);
        newUser.setActivationTokenExpiration(expiration);

        User savedUser = userRepository.save(newUser);

        String activationLink = "http://localhost:8088/api/v1/users/activate?token=" + activationToken;
        String content = "<h3>Xin chào, Chào mừng bạn đến với BookTour!</h3>" +
                "<p>Cảm ơn bạn đã đăng ký tài khoản. Vui lòng nhấp vào liên kết dưới đây để kích hoạt tài khoản của bạn:</p>" +
                "<p><a href=\"" + activationLink + "\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Kích hoạt tài khoản</a></p>" +
                "<p>Liên kết này sẽ có hiệu lực trong vòng 24 giờ. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>" +
                "<p>Trân trọng,<br>Đội ngũ BookTour</p>";
        try {
            emailService.sendInvoiceEmail(email, "Kích hoạt tài khoản", content, null);
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi email kích hoạt: " + e.getMessage());
        }

        return savedUser;
    }

    @Override
    public LoginResponse login(String userName, String password) throws Exception {
        Optional<User> userOptional = userRepository.findByUserName(userName);
        if (userOptional.isEmpty()) {
            throw new DataNotFoundException("Tên đăng nhập không tồn tại");
        }
        User existingUser = userOptional.get();

        if (!existingUser.getIsActive()) {
            throw new AccountBlockedException("Tài khoản đã bị chặn. Vui lòng liên hệ quản trị viên.");
        }

        if (!existingUser.getIsActivated()) {
            throw new AccountNotActivatedException("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để kích hoạt.");
        }

        if (existingUser.getFacebookAccountId() == 0 && existingUser.getGoogleAccountId() == 0) {
            if (!passwordEncoder.matches(password, existingUser.getPassword())) {
                throw new InvalidPasswordException("Mật khẩu không đúng");
            }
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userName, password, existingUser.getAuthorities());
        authenticationManager.authenticate(authenticationToken);

        String token = jwtTokenUtil.generateToken(existingUser);
        Long roleId = existingUser.getRole().getRoleId();
        Long userId = existingUser.getUserId();
        return LoginResponse.builder()
                .token(token)
                .roleId(roleId)
                .userId(userId)
                .build();
    }

    @Override
    @Transactional
    public User updateUser(Long userId, UpdateUserDTO updatedUserDTO) throws Exception {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));
        String oldPassword = updatedUserDTO.getOldPassword();

        if (oldPassword != null && !passwordEncoder.matches(oldPassword, existingUser.getPassword())) {
            throw new DataNotFoundException("Mật khẩu cũ không đúng");
        }

        if (updatedUserDTO.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(updatedUserDTO.getPhoneNumber());
        }
        if (updatedUserDTO.getFullName() != null) {
            existingUser.setFullName(updatedUserDTO.getFullName());
        }
        if (updatedUserDTO.getAddress() != null) {
            existingUser.setAddress(updatedUserDTO.getAddress());
        }
        if (updatedUserDTO.getDateOfBirth() != null) {
            existingUser.setDateOfBirth(updatedUserDTO.getDateOfBirth());
        }
        if (updatedUserDTO.getFacebookAccountId() != null && updatedUserDTO.getFacebookAccountId() > 0) {
            existingUser.setFacebookAccountId(updatedUserDTO.getFacebookAccountId());
        }
        if (updatedUserDTO.getGoogleAccountId() != null && updatedUserDTO.getGoogleAccountId() > 0) {
            existingUser.setGoogleAccountId(updatedUserDTO.getGoogleAccountId());
        }

        String newPassword = updatedUserDTO.getNewPassword();
        String confirmPassword = updatedUserDTO.getConfirmPassword();
        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                throw new DataNotFoundException("Mật khẩu mới và xác nhận mật khẩu không khớp");
            }
            String encodedPassword = passwordEncoder.encode(newPassword);
            existingUser.setPassword(encodedPassword);

            List<Token> tokens = tokenRepository.findByUser(existingUser);
            for (Token token : tokens) {
                tokenRepository.delete(token);
            }
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public User updateAvatar(Long userId, MultipartFile avatar) throws Exception {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (avatar != null && !avatar.isEmpty()) {
            String fileName = storeFile(avatar);
            if (fileName != null) {
                existingUser.setAvatar("/uploads/avatars/" + fileName);
            }
        }

        return userRepository.save(existingUser);
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (file.getSize() == 0) {
            return null;
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh đại diện không được vượt quá 5MB");
        }

        Tika tika = new Tika();
        String mimeType = tika.detect(file.getInputStream());
        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là hình ảnh");
        }

        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        Path uploadDir = Paths.get(AVATAR_UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path destination = Paths.get(uploadDir.toString(), fileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @Override
    public Page<User> getAllUser(String keyword, Pageable pageable) {
        return userRepository.findAll(keyword, pageable);
    }

    @Override
    @Transactional
    public void blockOrEnable(Long userId, Boolean active) throws DataNotFoundException {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));
        existingUser.setIsActive(active);
        userRepository.save(existingUser);
    }

    @Override
    public UserProfileResponse getUserProfile(Long userId) throws Exception {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));

        UserResponse userResponse = UserResponse.fromUser(existingUser);

        return UserProfileResponse.builder()
                .userName(existingUser.getUsername())
                .fullName(existingUser.getFullName())
                .phoneNumber(existingUser.getPhoneNumber())
                .email(existingUser.getEmail())
                .avatarPath(userResponse.getAvatar())
                .address(existingUser.getAddress())
                .dateOfBirth(existingUser.getDateOfBirth())
                .facebookAccountId(existingUser.getFacebookAccountId())
                .googleAccountId(existingUser.getGoogleAccountId())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(Long id) throws DataNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + id));

        List<Token> tokens = tokenRepository.findByUser(user);
        tokenRepository.deleteAll(tokens);

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void initiateResetPassword(String email) throws DataNotFoundException, IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("Email không tồn tại"));

        String resetToken = generateRandomCode();
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(10);
        tokenRepository.deleteAllByUser(user);

        Token tokenEntity = Token.builder()
                .token(resetToken)
                .tokenType("RESET_PASSWORD")
                .expirationDate(expiration)
                .revoked(false)
                .expired(false)
                .user(user)
                .build();
        tokenRepository.save(tokenEntity);

        String content = "<h3>Xin chào,</h3>" +
                "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản BookTour của bạn. Vui lòng sử dụng mã xác nhận dưới đây để tiếp tục:</p>" +
                "<p style=\"font-size: 18px; font-weight: bold; color: #4CAF50;\">" + resetToken + "</p>" +
                "<p>Mã này có hiệu lực trong vòng 10 phút. Vui lòng nhập mã vào trang đặt lại mật khẩu để hoàn tất quá trình.</p>" +
                "<p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>" +
                "<p>Trân trọng,<br>Đội ngũ BookTour</p>";
        try {
            emailService.sendInvoiceEmail(email, "Yêu cầu đặt lại mật khẩu", content, null);
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi email đặt lại mật khẩu: " + e.getMessage());
        }
    }

    private String generateRandomCode() {
        int code = 100000 + random.nextInt(900000);
        String resetToken = String.valueOf(code);
        while (tokenRepository.existsByToken(resetToken)) {
            code = 100000 + random.nextInt(900000);
            resetToken = String.valueOf(code);
        }
        return resetToken;
    }

    @Override
    public void verifyResetToken(String tokenStr) throws DataNotFoundException {
        Token token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new DataNotFoundException("Mã token không hợp lệ."));

        if (token.isExpired() || token.isRevoked() || token.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã token đã hết hạn hoặc không hợp lệ.");
        }
    }

    @Override
    public void resetPasswordWithToken(String tokenStr, String newPassword) throws DataNotFoundException {
        Token token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new DataNotFoundException("Mã token không hợp lệ."));

        if (token.isExpired() || token.isRevoked() || token.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã token đã hết hạn hoặc không hợp lệ.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setRevoked(true);
        token.setExpired(true);
        tokenRepository.save(token);
    }

    @Override
    @Transactional
    public void activateAccount(String token) throws DataNotFoundException {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new DataNotFoundException("Mã token không hợp lệ."));

        if (user.getIsActivated()) {
            throw new RuntimeException("Tài khoản đã được kích hoạt.");
        }
        if (user.getActivationTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã token đã hết hạn.");
        }

        user.setIsActivated(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiration(null);
        userRepository.save(user);

        String content = "<h3>Xin chào,</h3>" +
                "<p>Chúc mừng bạn! Tài khoản BookTour của bạn đã được kích hoạt thành công.</p>" +
                "<p>Bạn có thể đăng nhập ngay bây giờ bằng cách nhấp vào liên kết sau:</p>" +
                "<p><a href=\"http://localhost:5173/login\" style=\"background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Đăng nhập</a></p>" +
                "<p>Nếu bạn gặp bất kỳ vấn đề nào, vui lòng liên hệ với chúng tôi qua email hỗ trợ.</p>" +
                "<p>Trân trọng,<br>Đội ngũ BookTour</p>";
        try {
            if (user.getEmail() != null) {
                emailService.sendInvoiceEmail(user.getEmail(), "Tài khoản đã được kích hoạt", content, null);
            } else {
                System.err.println("Không thể gửi email vì địa chỉ email của người dùng là null.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi email thông báo kích hoạt: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resendActivationEmail(String email) throws DataNotFoundException, IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("Email không tồn tại."));
        if (user.getIsActivated()) {
            throw new RuntimeException("Tài khoản đã được kích hoạt.");
        }

        String activationToken = UUID.randomUUID().toString();
        LocalDateTime expiration = LocalDateTime.now().plusHours(24);
        user.setActivationToken(activationToken);
        user.setActivationTokenExpiration(expiration);
        userRepository.save(user);

        String activationLink = "http://localhost:8088/api/v1/users/activate?token=" + activationToken;
        String content = "<h3>Xin chào,</h3>" +
                "<p>Chúng tôi đã nhận được yêu cầu gửi lại liên kết kích hoạt tài khoản BookTour của bạn. Vui lòng nhấp vào liên kết dưới đây để kích hoạt:</p>" +
                "<p><a href=\"" + activationLink + "\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Kích hoạt tài khoản</a></p>" +
                "<p>Sau khi kích hoạt thành công, bạn có thể đăng nhập tại đây:</p>" +
                "<p><a href=\"http://localhost:5173/login\" style=\"background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Đăng nhập</a></p>" +
                "<p>Liên kết kích hoạt sẽ có hiệu lực trong vòng 24 giờ. Nếu bạn không yêu cầu gửi lại liên kết, vui lòng bỏ qua email này.</p>" +
                "<p>Trân trọng,<br>Đội ngũ BookTour</p>";
        try {
            emailService.sendInvoiceEmail(email, "Kích hoạt tài khoản", content, null);
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi email kích hoạt: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void activateAccountByAdmin(Long userId) throws DataNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (user.getIsActivated()) {
            throw new RuntimeException("Tài khoản đã được kích hoạt.");
        }

        user.setIsActivated(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiration(null);
        userRepository.save(user);

        String content = "<h3>Xin chào,</h3>" +
                "<p>Chúng tôi xin thông báo rằng tài khoản BookTour của bạn đã được kích hoạt thành công bởi quản trị viên.</p>" +
                "<p>Bạn có thể đăng nhập ngay bây giờ bằng cách nhấp vào liên kết sau:</p>" +
                "<p><a href=\"http://localhost:5173/login\" style=\"background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Đăng nhập</a></p>" +
                "<p>Nếu bạn gặp bất kỳ vấn đề nào, vui lòng liên hệ với chúng tôi qua email hỗ trợ.</p>" +
                "<p>Trân trọng,<br>Đội ngũ BookTour</p>";
        try {
            if (user.getEmail() != null) {
                emailService.sendInvoiceEmail(user.getEmail(), "Tài khoản đã được kích hoạt", content, null);
            } else {
                System.err.println("Không thể gửi email vì địa chỉ email của người dùng là null.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi email thông báo kích hoạt: " + e.getMessage());
        }
    }
}