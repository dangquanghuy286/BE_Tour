package com.project.booktour.controllers;

import com.project.booktour.responses.invoice.InvoiceResponse;
import com.project.booktour.services.invoice.IInvoiceService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("${api.prefix}/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final IInvoiceService invoiceService;

    @GetMapping("")
    public ResponseEntity<?> getOrCreateInvoice(
            @RequestParam("bookingId") Long bookingId) {
        try {
            InvoiceResponse response = invoiceService.getOrCreateInvoice(bookingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/send/{bookingId}")
    public ResponseEntity<?> sendInvoiceToCustomer(
            @PathVariable("bookingId") Long bookingId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            String result = invoiceService.sendInvoiceToCustomer(bookingId, file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý file: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi gửi email: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}