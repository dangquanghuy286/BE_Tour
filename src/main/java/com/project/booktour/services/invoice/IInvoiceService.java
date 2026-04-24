package com.project.booktour.services.invoice;

import com.project.booktour.responses.invoice.InvoiceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IInvoiceService {
    InvoiceResponse getOrCreateInvoice(Long bookingId);
    String sendInvoiceToCustomer(Long bookingId, MultipartFile file) throws IOException;
}