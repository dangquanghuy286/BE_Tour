package com.project.booktour.responses.toursreponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.booktour.models.Tour;
import com.project.booktour.models.TourImage;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopBookedTourResponse {

    private Long id;
    private String title;
    private String duration;
    private String img;
    private String destination;
    private int availableSlots;
    private String price_adult;
    private Float star;
    @JsonProperty("count_star") // Thêm trường countStar
    private Map<Integer, Long> countStar;

    @JsonProperty("tag")
    public String getTag() {
        try {
            String cleanedPrice = price_adult.replace(".", "").replace(" VNĐ", "").trim();
            double price = Double.parseDouble(cleanedPrice);
            if (price <= 3_000_000) {
                return "Economy";
            } else if (price <= 5_000_000) {
                return "Standard";
            } else {
                return "Premium";
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing price_adult: " + price_adult + ", error: " + e.getMessage());
            return "Unknown";
        }
    }

    public static TopBookedTourResponse fromTour(Tour tour, int availableSlots, Float averageRating, Map<Integer, Long> countStar) {
        String imageUrl = null;
        if (tour.getTourImages() != null && !tour.getTourImages().isEmpty()) {
            TourImage firstImage = tour.getTourImages().get(0);
            imageUrl = "http://localhost:8088/api/v1/tours/images/" + firstImage.getImageUrl();
        } else {
            imageUrl = "http://localhost:8088/api/v1/tours/images/notfound.jpeg";
        }

        TopBookedTourResponse response = new TopBookedTourResponse();
        response.setId(tour.getTourId());
        response.setDestination(tour.getDestination());
        response.setAvailableSlots(availableSlots);
        response.setTitle(tour.getTitle());
        response.setPrice_adult(String.format("%,.0f VNĐ", tour.getPriceAdult()));
        response.setDuration(tour.getDuration());
        response.setImg(imageUrl);
        response.setStar(averageRating != null ? averageRating : 0.0f);
        response.setCountStar(countStar); // Thêm countStar
        return response;
    }
}