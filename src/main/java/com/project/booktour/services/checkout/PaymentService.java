package com.project.booktour.services.checkout;

import com.project.booktour.components.VNPayUtil;
import com.project.booktour.configurations.VNPayConfig;
import com.project.booktour.models.Booking;
import com.project.booktour.models.BookingStatus;
import com.project.booktour.models.Checkout;
import com.project.booktour.models.PaymentStatus;
import com.project.booktour.repositories.BookingRepository;
import com.project.booktour.repositories.CheckoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final VNPayConfig vnPayConfig;
    private final BookingRepository bookingRepository;
    private final CheckoutRepository checkoutRepository;

    @Transactional
    public String createPaymentUrl(Long bookingId, String ipAddress) throws Exception {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getBookingStatus().equals(BookingStatus.PENDING)) {
            throw new RuntimeException("Booking is not in PENDING status");
        }

        // Kiểm tra xem đã tồn tại Checkout cho booking này chưa
        Optional<Checkout> existingCheckout = checkoutRepository.findByBookingBookingId(bookingId);
        if (!existingCheckout.isPresent()) {
            throw new RuntimeException("Checkout record not found for bookingId: " + bookingId);
        }

        Checkout checkout = existingCheckout.get();
        // Cập nhật thông tin Checkout nếu cần
        checkout.setPaymentMethod("VNPAY");
        checkout.setPaymentDetails("Initiated VNPAY payment");
        checkout.setAmount(booking.getTotalPrice());
        checkout.setPaymentStatus(PaymentStatus.PENDING);
        checkout.setTransactionId("BOOK-" + bookingId);
        checkout.setPaymentDate(LocalDateTime.now());
        checkoutRepository.save(checkout);

        String orderId = "BOOK-" + bookingId;
        double amount = booking.getTotalPrice();

        return VNPayUtil.generatePaymentUrl(vnPayConfig, orderId, amount, ipAddress);
    }

    @Transactional
    public void handlePaymentCallback(Map<String, String> params) throws Exception {
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        String vnp_SecureHash = params.get("vnp_SecureHash");

        // Verify checksum
        boolean isValid = VNPayUtil.verifyChecksum(vnPayConfig, params, vnp_SecureHash);
        if (!isValid) {
            throw new RuntimeException("Invalid checksum");
        }

        // Extract booking ID from TxnRef (format: BOOK-<bookingId>)
        String[] parts = vnp_TxnRef.split("-");
        if (parts.length != 2 || !parts[0].equals("BOOK")) {
            throw new RuntimeException("Invalid TxnRef format");
        }
        Long bookingId = Long.parseLong(parts[1]);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        Checkout checkout = checkoutRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Checkout not found"));

        // Cập nhật trạng thái dựa trên kết quả thanh toán
        if ("00".equals(vnp_TransactionStatus)) {
            checkout.setPaymentStatus(PaymentStatus.COMPLETED);
            checkout.setPaymentDetails("Transaction No: " + params.get("vnp_TransactionNo"));
            checkout.setAmount(Double.parseDouble(params.get("vnp_Amount")) / 100);
            checkout.setTransactionId("BOOK-" + params.get("vnp_TransactionNo"));
            checkout.setPaymentDate(LocalDateTime.now());

            // Đồng bộ booking_status nếu đang PENDING
            if (booking.getBookingStatus() == BookingStatus.PENDING) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
            }
        } else {
            // Không thay đổi booking_status nếu thanh toán thất bại (giữ PENDING)
        }

        bookingRepository.save(booking);
        checkoutRepository.save(checkout);
    }
}