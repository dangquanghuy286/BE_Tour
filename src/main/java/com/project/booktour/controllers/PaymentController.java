package com.project.booktour.controllers;

import com.project.booktour.services.checkout.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("${api.prefix}/payment")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());
    private final PaymentService paymentService;

    @PostMapping("/create-payment/{bookingId}")
    public ResponseEntity<String> createPayment(@PathVariable Long bookingId, HttpServletRequest request) throws Exception {
        String ipAddress = request.getRemoteAddr();
        logger.info("Initiating payment for bookingId: " + bookingId + ", IP: " + ipAddress);
        String paymentUrl = paymentService.createPaymentUrl(bookingId, ipAddress);
        return ResponseEntity.ok(paymentUrl);
    }

    @GetMapping("/vnpay-payment-callback")
    public ResponseEntity<String> paymentCallback(@RequestParam Map<String, String> params) throws Exception {
        logger.info("Received VNPay callback: " + params);
        paymentService.handlePaymentCallback(params);
        String status = params.get("vnp_TransactionStatus");
        String result = "00".equals(status) ? "success" : "failure";
        String redirectUrl = "http://localhost:5173/payment-result?status=" +
                URLEncoder.encode(result, StandardCharsets.UTF_8);
        logger.info("Redirecting to: " + redirectUrl);
        return ResponseEntity.ok(redirectUrl);
    }
}