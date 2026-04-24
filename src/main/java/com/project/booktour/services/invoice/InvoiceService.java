package com.project.booktour.services.invoice;

import com.project.booktour.models.*;
import com.project.booktour.repositories.BookingRepository;
import com.project.booktour.repositories.InvoiceRepository;
import com.project.booktour.responses.invoice.InvoiceResponse;
import com.project.booktour.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    private static final String INVOICE_UPLOAD_DIR = "uploads/invoices/";

    @Override
    public InvoiceResponse getOrCreateInvoice(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseGet(() -> createNewInvoice(booking));

        return buildInvoiceResponse(booking, invoice);
    }

    @Override
    public String sendInvoiceToCustomer(Long bookingId, MultipartFile file) throws IOException {
        try {
            if (bookingId == null) {
                throw new IllegalArgumentException("Booking ID cannot be null");
            }

            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

            if (booking.getEmail() == null || booking.getEmail().isEmpty()) {
                throw new RuntimeException("User information or email is missing for booking: " + bookingId);
            }

            if (file != null && file.isEmpty()) {
                throw new IllegalArgumentException("File đính kèm rỗng");
            }

            String emailContent = buildEmailContent(booking);

            System.out.println("Sending email to: " + booking.getEmail());
            System.out.println("Subject: Hóa đơn đặt tour #" + bookingId);
            System.out.println("Attachment: " + (file != null ? file.getOriginalFilename() : "None"));

            emailService.sendInvoiceEmail(
                    booking.getEmail(),
                    "Hóa đơn đặt tour #" + bookingId,
                    emailContent,
                    file
            );

            return "Invoice sent successfully to " + booking.getEmail();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new IOException("Failed to send invoice: " + e.getMessage(), e);
        }
    }

    private Invoice createNewInvoice(Booking booking) {
        Tour tour = booking.getTour();
        if (tour == null) {
            throw new RuntimeException("Tour not found for booking");
        }

        List<Checkout> checkouts = booking.getCheckouts();
        Checkout checkout = checkouts != null && !checkouts.isEmpty() ? checkouts.get(0) : null;

        String details = buildInvoiceDetails(booking, tour, checkout);

        Invoice invoice = Invoice.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .invoiceDate(LocalDate.now())
                .details(details)
                .build();

        return invoiceRepository.save(invoice);
    }

    private String buildInvoiceDetails(Booking booking, Tour tour, Checkout checkout) {
        String fullName = booking.getUser() != null ?
                truncate(booking.getUser().getFullName(), 50) : "Unknown";
        String tourTitle = truncate(tour.getTitle(), 50);
        String transactionId = checkout != null ?
                truncate(checkout.getTransactionId(), 30) : "N/A";
        String paymentStatus = checkout != null ?
                checkout.getPaymentStatus().name() : "N/A";

        String details = String.format(
                "Customer: %s, Tour: %s, Total: %.2f, PayStatus: %s, TxID: %s",
                fullName, tourTitle, booking.getTotalPrice(), paymentStatus, transactionId
        );

        return truncate(details, 255);
    }

    private String formatUserId(Long userId) {
        return userId != null ? "User" + userId : null;
    }

    private InvoiceResponse buildInvoiceResponse(Booking booking, Invoice invoice) {
        Tour tour = booking.getTour();
        Checkout checkout = booking.getCheckouts() != null && !booking.getCheckouts().isEmpty() ?
                booking.getCheckouts().get(0) : null;

        return InvoiceResponse.builder()
                .bookingId(booking.getBookingId())
                .numAdults(booking.getNumAdults())
                .numChildren(booking.getNumChildren())
                .priceAdults(String.format("%,.0f VNĐ", tour.getPriceAdult()))
                .priceChild(String.format("%,.0f VNĐ", tour.getPriceChild()))
                .totalPrice(String.format("%,.0f VNĐ", booking.getTotalPrice()))
                .fullName(booking.getUser() != null ? booking.getFullName() : null)
                .address(booking.getUser() != null ? booking.getAddress() : null)
                .phoneNumber(booking.getUser() != null ? booking.getPhoneNumber() : null)
                .email(booking.getUser() != null ? booking.getEmail() : null)
                .departurePoint(tour.getDeparturePoint() != null ? tour.getDeparturePoint() : null)
                .createdAt(invoice.getCreatedAt())
                .bookingStatus(booking.getBookingStatus().name())
                .paymentMethod(checkout != null ? checkout.getPaymentMethod() : null)
                .paymentStatus(checkout != null ? checkout.getPaymentStatus().name() : null)
                .transactionId(checkout != null ? checkout.getTransactionId() : null)
                .updatedAt(invoice.getUpdatedAt())
                .account(null)
                .tax(0.0)
                .discount(booking.getPromotion() != null ? booking.getPromotion().getDiscount() : 0.0)
                .title(tour != null ? tour.getTitle() : null)
                .specialRequests(booking.getSpecialRequests())
                .formattedTourId(String.format("Tour%03d", tour.getTourId()))
                .userId(booking.getUser() != null ? booking.getUser().getUserId() : null)
                .formattedUserId(booking.getUser() != null ? formatUserId(booking.getUser().getUserId()) : null)
                .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                .build();
    }

    private File saveUploadedFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(INVOICE_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return filePath.toFile();
    }

    private String buildEmailContent(Booking booking) {
        String startDate = booking.getTour() != null && booking.getTour().getStartDate() != null
                ? booking.getTour().getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
        String endDate = booking.getTour() != null && booking.getTour().getEndDate() != null
                ? booking.getTour().getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";
        String fullName = booking.getFullName() != null ? booking.getFullName() : "Quý khách";

        String promotionInfo = "";
        if (booking.getPromotion() != null) {
            Promotion promotion = booking.getPromotion();
            promotionInfo = String.format(
                    "<li><strong>Mã giảm giá:</strong> %s (%s%%, hết hạn: %s)</li>",
                    promotion.getCode(),
                    promotion.getDiscount(),
                    promotion.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        }

        return "<h3>Xin chào " + fullName + ",</h3>" +
                "<p>Cảm ơn bạn đã tin tưởng và lựa chọn dịch vụ của chúng tôi.</p>" +
                "<p>Chúng tôi xin gửi đến bạn thông tin chi tiết về tour du lịch bạn đã đặt:</p>" +
                "<ul>" +
                "<li><strong>Ngày khởi hành:</strong> " + startDate + "</li>" +
                "<li><strong>Ngày kết thúc:</strong> " + endDate + "</li>" +
                promotionInfo +
                "<li><strong>Tổng tiền:</strong> " + String.format("%,.0f VNĐ", booking.getTotalPrice()) + "</li>" +
                "</ul>" +
                "<p>Chúng tôi rất mong được đồng hành cùng bạn trong chuyến đi sắp tới và cam kết mang đến cho bạn những trải nghiệm tuyệt vời nhất.</p>" +
                "<p>Trân trọng,</p>" +
                "<p><em>Đội ngũ hỗ trợ khách hàng - Công ty Du lịch</em></p>";
    }

    private String truncate(String value, int length) {
        if (value == null) return null;
        return value.length() > length ? value.substring(0, length) : value;
    }
}