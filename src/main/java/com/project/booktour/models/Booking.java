package com.project.booktour.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "booking")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Booking extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "num_adults", nullable = false)
    private Integer numAdults;

    @Column(name = "num_children", nullable = false)
    private Integer numChildren;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private BookingStatus bookingStatus;

    @Column(name = "special_requests", length = 255)
    private String specialRequests;

    @ManyToOne
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;
    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Checkout> checkouts;
    @NotBlank(message = "Full name is required")
    @Column(name = "full_name", length = 50, nullable = false)
    private String fullName;

    @NotBlank(message = "Address is required")
    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @NotBlank(message = "Phone number is required")
    @Column(name = "phone_number", length = 15, nullable = false)
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Column(name = "email", length = 255, nullable = false)
    private String email;


}