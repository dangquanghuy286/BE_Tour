package com.project.booktour.services.review;

import com.project.booktour.dtos.ReviewDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.exceptions.UnauthorizedException;
import com.project.booktour.models.Booking;
import com.project.booktour.models.BookingStatus;
import com.project.booktour.models.Review;
import com.project.booktour.models.Tour;
import com.project.booktour.models.User;
import com.project.booktour.repositories.BookingRepository;
import com.project.booktour.repositories.ReviewRepository;
import com.project.booktour.repositories.TourRepository;
import com.project.booktour.repositories.UserRepository;
import com.project.booktour.responses.reviewsreponse.ReviewResponse;
import com.project.booktour.services.booking.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @Override
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Override
    public Review createReview(ReviewDTO reviewDTO) throws Exception {
        Long userId = reviewDTO.getUserId();
        Long tourId = reviewDTO.getTourId();

        // Lấy tour để kiểm tra endDate
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với ID: " + tourId));

        // Lấy danh sách booking
        List<Booking> bookings = bookingRepository.findByUserUserIdAndTourTourId(userId, tourId);
        if (bookings.isEmpty()) {
            throw new UnauthorizedException("Bạn chưa đặt tour này và không thể đánh giá.");
        }

        // Đếm số booking đã hoàn thành
        int completedBookings = 0;
        for (Booking booking : bookings) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED && tour.getEndDate().isBefore(LocalDate.now())) {
                booking.setBookingStatus(BookingStatus.COMPLETED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
                completedBookings++;
            } else if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
                completedBookings++;
            }
        }

        if (completedBookings == 0) {
            throw new UnauthorizedException("Bạn chỉ có thể đánh giá sau khi hoàn thành tour.");
        }

        // Lấy số lượng review đã đánh giá tour này
        int existingReviews = reviewRepository.findByTourTourIdAndUserUserId(tourId, userId).size();

        if (existingReviews >= completedBookings) {
            throw new InvalidParamException("Bạn đã đánh giá đủ số lần theo số tour đã hoàn thành.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Review review = Review.builder()
                .tour(tour)
                .user(user)
                .comment(reviewDTO.getComment())
                .rating(reviewDTO.getRating())
                .build();

        return reviewRepository.save(review);
    }

    @Override
    public void updateReview(Long reviewId, ReviewDTO reviewDTO) throws Exception {
        // Kiểm tra xem người dùng có booking cho tour này không
        if (!bookingService.hasUserBookedTour(reviewDTO.getUserId(), reviewDTO.getTourId())) {
            throw new UnauthorizedException("Bạn chưa đặt tour này và không thể cập nhật đánh giá.");
        }

        // Lấy tour
        Tour tour = tourRepository.findById(reviewDTO.getTourId())
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với ID: " + reviewDTO.getTourId()));

        // Cập nhật trạng thái booking nếu tour đã kết thúc (tùy chọn, để đảm bảo nhất quán)
        List<Booking> bookings = bookingRepository.findByUserUserIdAndTourTourId(reviewDTO.getUserId(), reviewDTO.getTourId());
        for (Booking booking : bookings) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED && tour.getEndDate().isBefore(LocalDate.now())) {
                booking.setBookingStatus(BookingStatus.COMPLETED);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }
        }

        // Lấy và kiểm tra đánh giá
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        // Kiểm tra quyền sở hữu đánh giá
        if (!existingReview.getUser().getUserId().equals(reviewDTO.getUserId())) {
            throw new UnauthorizedException("Bạn không có quyền cập nhật đánh giá của người khác.");
        }

        // Cập nhật thông tin đánh giá
        existingReview.setTour(tour);
        existingReview.setComment(reviewDTO.getComment());
        existingReview.setRating(reviewDTO.getRating());

        reviewRepository.save(existingReview);
    }

    @Override
    public List<Review> getReviewsByTour(Long tourId) {
        return reviewRepository.findByTourTourId(tourId);
    }

    @Override
    public List<Review> getReviewsByUserAndTour(Long userId, Long tourId) {
        return reviewRepository.findByTourTourIdAndUserUserId(tourId, userId);
    }

    @Override
    public List<ReviewResponse> getReviewListByTour(Long tourId) throws DataNotFoundException {
        if (!tourRepository.existsById(tourId)) {
            throw new DataNotFoundException("Không tìm thấy tour với ID: " + tourId);
        }

        List<Review> reviews = reviewRepository.findByTourTourId(tourId);
        return reviews.stream()
                .map(ReviewResponse::fromReview)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) throws Exception {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xóa đánh giá của người khác.");
        }

        reviewRepository.delete(review);
    }
}