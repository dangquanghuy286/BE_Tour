package com.project.booktour.repositories;

import com.project.booktour.models.Checkout;
import com.project.booktour.models.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Query("SELECT i FROM Invoice i WHERE i.booking.id = :bookingId")
    Optional<Invoice> findByBookingId(Long bookingId);
}
