package com.project.booktour.responses.invoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    @JsonProperty("booking_id")
    private Long bookingId;

    @JsonProperty("num_adults")
    private Integer numAdults;

    @JsonProperty("num_children")
    private Integer numChildren;

    @JsonProperty("price_adults")
    private String priceAdults;

    @JsonProperty("price_child")
    private String priceChild;

    @JsonProperty("total_price")
    private String totalPrice;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("address")
    private String address;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("email")
    private String email;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("booking_status")
    private String bookingStatus;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("account")
    private String account;

    @JsonProperty("tax")
    private Double tax;

    @JsonProperty("discount")
    private Double discount;

    @JsonProperty("title")
    private String title;

    @JsonProperty("special_requests")
    private String specialRequests;

    @JsonProperty("departure_point")
    private String departurePoint;

    @JsonProperty("formatted_tour_id")
    private String formattedTourId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("formatted_user_id")
    private String formattedUserId;

    @JsonProperty("promotion_code")
    private String promotionCode;
}