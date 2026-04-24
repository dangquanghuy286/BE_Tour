package com.project.booktour.responses.toursreponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.booktour.dtos.ScheduleDTO;
import com.project.booktour.models.Tour;
import com.project.booktour.responses.BaseResponse;
import com.project.booktour.responses.reviewsreponse.ReviewResponse;
import lombok.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourResponse extends BaseResponse {
    private Long id;
    private String title;
    @JsonProperty("price_adult")
    private String priceAdult;
    @JsonProperty("price_child")
    private String priceChild;
    @JsonProperty("img")
    private List<String> images;
    private int availableSlots;
    private int quantity;
    private String description;
    private String duration;
    private String destination;
    private boolean availability;
    private List<ScheduleDTO> itinerary;
    private List<String> include;
    private List<String> notinclude;
    private LocalDate startDate;
    private LocalDate endDate;
    @JsonProperty("reviews")
    private List<ReviewResponse> reviews;
    @JsonProperty("average_rating")
    private Float averageRating;
    @JsonProperty("total_reviews")
    private Integer totalReviews;
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

    public static TourResponse fromTour(Tour tour, ObjectMapper objectMapper, List<String> imageUrls, List<ReviewResponse> reviews, Float averageRating, Integer totalReviews, int availableSlots, Map<Integer, Long> countStar) throws Exception {
        List<ScheduleDTO> itinerary = tour.getItinerary() != null
                ? objectMapper.readValue(tour.getItinerary(), new TypeReference<List<ScheduleDTO>>() {})
                : null;
        List<String> includeList = Arrays.asList(
                "Dịch vụ đón và trả khách tận nơi, đảm bảo hành trình thuận tiện và thoải mái",
                "Thưởng thức 1 bữa ăn mỗi ngày được chế biến tinh tế bởi các đầu bếp hàng đầu",
                "Trải nghiệm bữa tối sang trọng trên du thuyền với sự kiện âm nhạc sống động, đậm chất văn hóa",
                "Khám phá 7 điểm đến tuyệt đẹp, mang đến những khoảnh khắc đáng nhớ trong lòng thành phố",
                "Miễn phí nước đóng chai trên xe, luôn sẵn sàng giữ bạn sảng khoái suốt hành trình",
                "Di chuyển đẳng cấp với xe buýt du lịch hạng sang, tiện nghi và hiện đại"
        );

        List<String> notIncludeList = Arrays.asList(
                "Tiền boa cho hướng dẫn viên hoặc nhân viên (tùy thuộc vào sự hài lòng của quý khách)",
                "Dịch vụ đón và trả tại khách sạn (vui lòng liên hệ để biết thêm chi tiết)",
                "Bữa trưa, các món ăn và đồ uống ngoài thực đơn được cung cấp",
                "Tùy chọn nâng cấp trải nghiệm với một ly cocktail hoặc dịch vụ cao cấp",
                "Các dịch vụ bổ sung không nằm trong chương trình tour",
                "Bảo hiểm du lịch (khuyến nghị quý khách tự mua để đảm bảo an toàn)"
        );
        TourResponse tourResponse = TourResponse.builder()
                .id(tour.getTourId())
                .title(tour.getTitle())
                .priceAdult(String.format("%,.0f VNĐ", tour.getPriceAdult()))
                .priceChild(String.format("%,.0f VNĐ", tour.getPriceChild()))
                .images(imageUrls)
                .quantity(tour.getQuantity())
                .availableSlots(availableSlots)
                .description(tour.getDescription())
                .duration(tour.getDuration())
                .destination(tour.getDestination())
                .availability(tour.getAvailability())
                .include(includeList)
                .notinclude(notIncludeList)
                .itinerary(itinerary)
                .startDate(tour.getStartDate())
                .endDate(tour.getEndDate())
                .reviews(reviews)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .departurePoint(tour.getDeparturePoint())
                .countStar(countStar) // Thêm countStar
                .build();
        tourResponse.setCreatedAt(tour.getCreatedAt());
        tourResponse.setUpdatedAt(tour.getUpdatedAt());
        return tourResponse;
    }
}