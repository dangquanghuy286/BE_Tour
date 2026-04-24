package com.project.booktour.repositories;

import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.Booking;
import com.project.booktour.models.BookingStatus;
import com.project.booktour.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE b.tour.tourId = :tourId AND b.bookingStatus = :status")
    List<Booking> findByTourAndBookingStatus(@Param("tourId") Long tourId, @Param("status") BookingStatus status);
    boolean existsByUserUserIdAndTourTourId(Long userId, Long tourId);
    List<Booking> findByUserUserId(Long userId);

    @Query("SELECT b FROM Booking b JOIN b.tour t JOIN b.user u " +
            "WHERE :keyword IS NULL OR b.specialRequests LIKE %:keyword% OR b.bookingStatus LIKE %:keyword% " +
            "OR t.title LIKE %:keyword% OR u.fullName LIKE %:keyword%")
    Page<Booking> findAll(String keyword, Pageable pageable);

    @Query("SELECT t.region AS region, COUNT(b) AS count " +
            "FROM Booking b JOIN b.tour t " +
            "GROUP BY t.region")
    List<Object[]> countBookingsByRegion();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tour.tourId = :tourId")
    Long countBookingsByTourId(Long tourId);





    @Query("SELECT b FROM Booking b WHERE b.tour.tourId = :tourId AND b.bookingStatus IN :statuses")
    List<Booking> findByTourAndBookingStatusIn(@Param("tourId") Long tourId, @Param("statuses") List<BookingStatus> statuses);




    List<Booking> findTop5ByOrderByCreatedAtDesc();

    List<Booking> findByUserUserIdAndTourTourId(Long userId, Long tourId);

    List<Booking> findByUserUserIdAndBookingStatus(Long userId, BookingStatus status);

    boolean existsByUserUserIdAndPromotionPromotionIdAndBookingStatusIn(
            Long userId, Long promotionId, List<BookingStatus> statuses);

}