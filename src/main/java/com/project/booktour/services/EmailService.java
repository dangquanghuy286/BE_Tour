package com.project.booktour.services;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendInvoiceEmail(String to, String subject, String content, MultipartFile attachment)
            throws IOException {  // Chỉ cần throws IOException
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            if (attachment != null && !attachment.isEmpty()) {
                helper.addAttachment(
                        attachment.getOriginalFilename(),
                        new ByteArrayResource(attachment.getBytes())
                );
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            // Chuyển thành RuntimeException để không phải khai báo throws
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage(), e);
        }
    }
    public void sendResetCodeEmail(String to, String subject, String resetCode) {
        String content = "<h3>Mã xác nhận đặt lại mật khẩu</h3>" +
                "<p>Mã xác nhận của bạn là: <strong>" + resetCode + "</strong></p>" +
                "<p>Mã này có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>" +
                "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>";

        try {
            sendInvoiceEmail(to, subject, content, null);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi gửi email reset mật khẩu: " + e.getMessage(), e);
        }
    }

}