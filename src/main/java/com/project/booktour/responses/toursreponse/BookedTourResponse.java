package com.project.booktour.responses.toursreponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.booktour.models.*;
import com.project.booktour.responses.BaseResponse;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedTourResponse extends BaseResponse {
    private Long tourId;
    private String title;
    private String description;
    private String duration;

    @JsonProperty("total_price")
    private String totalPrice;

    @JsonProperty("image")
    private String image;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("booking_id")
    private Long bookingId;
    private String destination;
    private Double rating;
    private String paymentMethod;
    @JsonProperty("num_adults")
    private int numAdults;

    @JsonProperty("num_children")
    private int numChildren;
    @JsonProperty("booking_status")
    private BookingStatus bookingStatus;
    @JsonProperty("count_star") // Thêm trường countStar
    private Map<Integer, Long> countStar;

    public static BookedTourResponse fromBooking(Booking booking, ObjectMapper objectMapper, Map<Integer, Long> countStar) throws Exception {
        Tour tour = booking.getTour();
        if (tour == null) {
            return null;
        }

        // Lấy URL ảnh đầu tiên từ TourImage
        String imageUrl = null;
        if (tour.getTourImages() != null && !tour.getTourImages().isEmpty()) {
            TourImage firstImage = tour.getTourImages().get(0);
            imageUrl = "http://localhost:8088/api/v1/tours/images/" + firstImage.getImageUrl();
        }

        // Lấy checkout đầu tiên (hoặc theo logic tùy bạn)
        Checkout checkout = null;
        if (booking.getCheckouts() != null && !booking.getCheckouts().isEmpty()) {
            checkout = booking.getCheckouts().get(0); // hoặc logic chọn bản ghi mới nhất
        }

        // Tính rating trung bình
        Double averageRating = null;
        if (tour.getReviews() != null && !tour.getReviews().isEmpty()) {
            averageRating = tour.getReviews().stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
        }

        BookedTourResponse response = new BookedTourResponse();
        response.setTourId(tour.getTourId());
        response.setTitle(tour.getTitle());
        response.setDescription(tour.getDescription());
        response.setDuration(tour.getDuration());
        response.setTotalPrice(String.format("%,.0f VNĐ", booking.getTotalPrice()));
        if (checkout != null) {
            response.setPaymentMethod(checkout.getPaymentMethod());
        } else {
            response.setTotalPrice("Không có thông tin"); // fallback
            response.setPaymentMethod("Không có thông tin");
        }

        response.setImage(imageUrl);
        response.setStartDate(tour.getStartDate());
        response.setEndDate(tour.getEndDate());
        response.setDestination(tour.getDestination());
        response.setRating(averageRating);
        response.setBookingStatus(booking.getBookingStatus());
        response.setBookingId(booking.getBookingId());
        response.setNumAdults(booking.getNumAdults());
        response.setNumChildren(booking.getNumChildren());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        response.setCountStar(countStar); // Thêm countStar
        return response;
    }
}