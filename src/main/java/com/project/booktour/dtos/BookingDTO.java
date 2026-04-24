package com.project.booktour.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.booktour.models.BookingStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingDTO {
    @JsonProperty("booking_id")
    private Long bookingId;

    @Min(value = 1, message = "User ID must be greater than 0")
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("tour_id")
    private Long tourId;

    @JsonProperty("formatted_tour_id")
    private String formattedTourId;

    @JsonProperty("title")
    private String title;

    @Min(value = 0, message = "Number of adults must be at least 0")
    @JsonProperty("num_adults")
    private Integer numAdults;

    @Min(value = 0, message = "Number of children must be at least 0")
    @JsonProperty("num_children")
    private Integer numChildren;

    @Min(value = 0, message = "Total price must be at least 0")
    @JsonProperty("total_price")
    private Double totalPrice;

    @NotNull(message = "Booking status is required")
    @JsonProperty("booking_status")
    private BookingStatus bookingStatus;

    @JsonProperty("special_requests")
    private String specialRequests;

    @JsonProperty("promotion_id")
    private Long promotionId;

    @JsonProperty("promotion_code")
    private String promotionCode;

    @JsonProperty("promotion_discount")
    private Double promotionDiscount; // Thêm trường tỷ lệ giảm giá

    @JsonProperty("promotion_description")
    private String promotionDescription; // Thêm trường mô tả khuyến mãi

    @NotBlank(message = "Full name is required")
    @JsonProperty("full_name")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Address is required")
    @JsonProperty("address")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid (10-15 digits)")
    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "^(VNPAY|OFFICE)$", message = "Payment method must be 'VNPAY' or 'OFFICE'")
    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("transaction_code")
    private String transactionCode; // Thêm trường mã giao dịch

    @JsonProperty("payment_date")
    private LocalDateTime paymentDate; // Thêm trường ngày thanh toán

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("price_adult")
    private String priceAdult;

    @JsonProperty("price_child")
    private String priceChild;

    @JsonProperty("departure_point")
    private String departurePoint;

    @JsonProperty("img")
    private String img;
}