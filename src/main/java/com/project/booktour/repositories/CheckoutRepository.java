package com.project.booktour.repositories;

import com.project.booktour.models.Checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckoutRepository extends JpaRepository<Checkout, Long> {
    Optional<Checkout> findByBookingBookingId(Long bookingId);

    @Query("SELECT c.paymentMethod, COUNT(c) FROM Checkout c WHERE c.booking IN (SELECT b FROM Booking b) GROUP BY c.paymentMethod")
    List<Object[]> countBookingsByPaymentMethod();

}