package com.project.booktour.responses.toursreponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.booktour.dtos.ScheduleDTO;
import com.project.booktour.models.Tour;
import com.project.booktour.responses.BaseResponse;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimplifiedTourResponse extends BaseResponse {
    private String id;
    @JsonProperty("img")
    private String image;
    private int availableSlots;
    private int quantity;
    private String title;
    private String description;
    private boolean availability;
    private Float star;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    @JsonProperty("price_adult")
    private String priceAdult;
    private List<ScheduleDTO> itinerary;
    @JsonProperty("price_child")
    private String priceChild;
    private String duration;
    private String region;
    private String departurePoint;
    @JsonProperty("count_star") // Thêm trường countStar
    private Map<Integer, Long> countStar;

    @JsonProperty("tag")
    public String getTag() {
        try {
            String cleanedPrice = priceAdult.replace(".", "").replace(" VNĐ", "").trim();
            double price = Double.parseDouble(cleanedPrice);
            if (price <= 3_000_000) {
                return "Economy";
            } else if (price <= 5_000_000) {
                return "Standard";
            } else {
                return "Premium";
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing priceAdult: " + priceAdult + ", error: " + e.getMessage());
            return "Unknown";
        }
    }

    public static SimplifiedTourResponse fromTour(Tour tour, ObjectMapper objectMapper, int availableSlots, Map<Integer, Long> countStar) throws Exception {
        List<ScheduleDTO> itinerary = tour.getItinerary() != null
                ? objectMapper.readValue(tour.getItinerary(), new TypeReference<List<ScheduleDTO>>() {})
                : null;
        SimplifiedTourResponse simplifiedTourResponse = new SimplifiedTourResponse();
        simplifiedTourResponse.setId(String.valueOf(tour.getTourId()));
        simplifiedTourResponse.setTitle(tour.getTitle());
        simplifiedTourResponse.setDestination(tour.getDestination());
        simplifiedTourResponse.setAvailableSlots(availableSlots);
        simplifiedTourResponse.setQuantity(tour.getQuantity());
        simplifiedTourResponse.setDescription(tour.getDescription());
        simplifiedTourResponse.setPriceAdult(String.format("%,.0f VNĐ", tour.getPriceAdult()));
        simplifiedTourResponse.setPriceChild(String.format("%,.0f VNĐ", tour.getPriceChild()));
        simplifiedTourResponse.setDuration(tour.getDuration());
        simplifiedTourResponse.setAvailability(tour.getAvailability());
        simplifiedTourResponse.setItinerary(itinerary);
        simplifiedTourResponse.setCreatedAt(tour.getCreatedAt());
        simplifiedTourResponse.setUpdatedAt(tour.getUpdatedAt());
        simplifiedTourResponse.setStartDate(tour.getStartDate());
        simplifiedTourResponse.setEndDate(tour.getEndDate());
        simplifiedTourResponse.setRegion(tour.getRegion().toString());
        simplifiedTourResponse.setDeparturePoint(tour.getDeparturePoint());
        simplifiedTourResponse.setCountStar(countStar); // Thêm countStar
        return simplifiedTourResponse;
    }
}