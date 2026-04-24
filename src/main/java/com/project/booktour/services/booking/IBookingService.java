package com.project.booktour.services.booking;

import com.project.booktour.dtos.BookingDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.Booking;
import com.project.booktour.models.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface IBookingService {
    Booking createBooking(BookingDTO bookingDTO) throws Exception;

    BookingDTO getBooking(Long id) throws DataNotFoundException;

    Booking updateBooking(Long id, BookingDTO bookingDTO) throws DataNotFoundException;

    void cancelBooking(Long id) throws DataNotFoundException;

    List<BookingDTO> findByUserId(Long userId) throws DataNotFoundException;
    boolean hasUserBookedTour(Long userId, Long tourId);
    Page<BookingDTO> getAllBookings(String keyword ,PageRequest pageRequest);
    Optional<Booking> findById(Long id) throws DataNotFoundException;
    // Thêm phương thức mới để lấy danh sách Booking (không phải DTO)
    List<Booking> findBookingsByUserId(Long userId);
    List<BookingDTO> findByUserIdAndStatus(Long userId, BookingStatus status) throws DataNotFoundException;
    boolean hasUserUsedPromotion(Long userId, Long promotionId);
}