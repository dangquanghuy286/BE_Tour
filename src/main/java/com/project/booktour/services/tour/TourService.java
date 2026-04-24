 package com.project.booktour.services.tour;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.booktour.dtos.TourDTO;
import com.project.booktour.dtos.TourImageDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.models.*;
import com.project.booktour.repositories.ReviewRepository;
import com.project.booktour.repositories.TourImageRepository;
import com.project.booktour.repositories.TourRepository;
import com.project.booktour.responses.reviewsreponse.ReviewResponse;
import com.project.booktour.responses.toursreponse.BookedTourResponse;
import com.project.booktour.responses.toursreponse.SimplifiedTourResponse;
import com.project.booktour.responses.toursreponse.TopBookedTourResponse;
import com.project.booktour.responses.toursreponse.TourResponse;
import com.project.booktour.services.booking.BookingService;
import com.project.booktour.services.review.ReviewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService implements ITourService {
    private static final Logger logger = LoggerFactory.getLogger(TourService.class);
    private final TourRepository tourRepository;
    private final TourImageRepository tourImageRepository;
    private final ObjectMapper objectMapper;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final BookingService bookingService;


    @Override
    public Tour createTour(TourDTO tourDTO) throws DataNotFoundException, JsonProcessingException, InvalidParamException {
        Tour newTour = Tour.builder()
                .title(tourDTO.getTitle())
                .description(tourDTO.getDescription())
                .quantity(tourDTO.getQuantity())
                .priceAdult(tourDTO.getPriceAdult())
                .priceChild(tourDTO.getPriceChild())
                .duration(tourDTO.getDuration())
                .destination(tourDTO.getDestination())
                .availability(tourDTO.isAvailability())
                .itinerary(tourDTO.getItinerary() != null ? objectMapper.writeValueAsString(tourDTO.getItinerary()) : null)
                .region(Region.valueOf(tourDTO.getRegion().toUpperCase()))
                .startDate(tourDTO.getStartDate())
                .endDate(tourDTO.getEndDate())
                .departurePoint(tourDTO.getDeparturePoint())
                .tourImages(new ArrayList<>())
                .build();

        return tourRepository.save(newTour);
    }

    @Override
    public Tour getTourById(Long tourId) throws Exception {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với id: " + tourId));
    }

    @Override
    public TourResponse getTourDetails(Long id) throws DataNotFoundException {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với id: " + id));

        List<String> imageUrls = tourImageRepository.findByTourTourId(id).stream()
                .map(TourImage::getImageUrl)
                .filter(Objects::nonNull)
                .map(url -> "http://localhost:8088/api/v1/tours/images/" + url)
                .collect(Collectors.toList());

        if (imageUrls.isEmpty()) {
            imageUrls = Collections.singletonList("http://localhost:8088/api/v1/tours/images/notfound.jpeg");
        }

        List<ReviewResponse> reviews = reviewService.getReviewListByTour(id);
        Float averageRating = reviewRepository.findAverageRatingByTourId(id).orElse(0.0f);
        Integer totalReviews = reviewRepository.countByTourTourId(id);

        // Tính countStar
        Map<Integer, Long> countStar = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            Long count = reviewRepository.countByTourTourIdAndRating(id, i);
            countStar.put(i, count);
        }

        Integer totalBookedTickets = tourRepository.getTotalBookedTicketsByTourId(id);
        int availableSlots = Math.max(0, tour.getQuantity() - (totalBookedTickets != null ? totalBookedTickets : 0));

        try {
            return TourResponse.fromTour(tour, objectMapper, imageUrls, reviews, averageRating, totalReviews, availableSlots, countStar);
        } catch (Exception e) {
            throw new RuntimeException("Không thể phân tích chi tiết tour với id: " + id, e);
        }
    }

    @Override
    public Page<SimplifiedTourResponse> getAllTours(PageRequest pageRequest, Double priceMin, Double priceMax, String region, Float starRating, String duration, String title, String tag, String departurePoint, String destination) {
        logger.info("Lấy danh sách tour với bộ lọc: priceMin={}, priceMax={}, region={}, starRating={}, duration={}, title={}, tag={}, departurePoint={}, destination={}",
                priceMin, priceMax, region, starRating, duration, title, tag, departurePoint, destination);

        if (tag != null) {
            switch (tag) {
                case "Economy":
                    priceMax = priceMax != null ? Math.min(priceMax, 3_000_000) : 3_000_000;
                    priceMin = priceMin != null ? Math.max(priceMin, 0) : 0;
                    break;
                case "Standard":
                    priceMin = priceMin != null ? Math.max(priceMin, 3_000_001) : 3_000_001;
                    priceMax = priceMax != null ? Math.min(priceMax, 5_000_000) : 5_000_000;
                    break;
                case "Premium":
                    priceMin = priceMin != null ? Math.max(priceMin, 5_000_001) : 5_000_001;
                    break;
            }
        }

        return tourRepository.findAllWithFilters(pageRequest, priceMin, priceMax, region, starRating, duration, title, departurePoint, destination)
                .map(result -> {
                    Tour tour = (Tour) result[0];
                    Double avgRating = (Double) result[1];
                    String firstImageUrl = (String) result[2];

                    Integer totalBookedTickets = tourRepository.getTotalBookedTicketsByTourId(tour.getTourId());
                    int availableSlots = Math.max(0, tour.getQuantity() - (totalBookedTickets != null ? totalBookedTickets : 0));

                    Map<Integer, Long> countStar = new HashMap<>();
                    for (int i = 1; i <= 5; i++) {
                        Long count = reviewRepository.countByTourTourIdAndRating(tour.getTourId(), i);
                        countStar.put(i, count);
                    }

                    try {
                        SimplifiedTourResponse response = SimplifiedTourResponse.fromTour(tour, objectMapper, availableSlots, countStar);
                        response.setStar(avgRating != null ? avgRating.floatValue() : 0.0f);

                        if (firstImageUrl != null && !firstImageUrl.isEmpty()) {
                            String baseUrl = "http://localhost:8088/api/v1/tours/images/";
                            response.setImage(baseUrl + firstImageUrl);
                        } else {
                            response.setImage("http://localhost:8088/api/v1/tours/images/notfound.jpeg");
                        }

                        return response;
                    } catch (Exception e) {
                        throw new RuntimeException("Không thể phân tích lịch trình cho tour với id: " + tour.getTourId(), e);
                    }
                });
    }
    @Override
    public Tour updateTour(Long id, TourDTO tourDTO) throws Exception {
        Tour existingTour = getTourById(id);
        if (existingTour != null) {
            if (!existingTour.getTitle().equals(tourDTO.getTitle()) && tourRepository.existsByTitle(tourDTO.getTitle())) {
                throw new InvalidParamException("Tour với tiêu đề '" + tourDTO.getTitle() + "' đã tồn tại");
            }

            existingTour.setTitle(tourDTO.getTitle());
            existingTour.setDescription(tourDTO.getDescription());
            existingTour.setQuantity(tourDTO.getQuantity());
            existingTour.setPriceAdult(tourDTO.getPriceAdult());
            existingTour.setPriceChild(tourDTO.getPriceChild());
            existingTour.setDuration(tourDTO.getDuration());
            existingTour.setDestination(tourDTO.getDestination());
            existingTour.setAvailability(tourDTO.isAvailability());
            existingTour.setItinerary(tourDTO.getItinerary() != null ? objectMapper.writeValueAsString(tourDTO.getItinerary()) : null);
            existingTour.setRegion(Region.valueOf(tourDTO.getRegion().toUpperCase()));
            existingTour.setStartDate(tourDTO.getStartDate());
            existingTour.setEndDate(tourDTO.getEndDate());
            existingTour.setDeparturePoint(tourDTO.getDeparturePoint());

            return tourRepository.save(existingTour);
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteTour(Long id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour với id: " + id));

        tourImageRepository.deleteAll(tour.getTourImages());
        reviewRepository.deleteAll(tour.getReviews());
        tourRepository.delete(tour);
    }

    @Override
    public boolean existsByTitle(String title) {
        return tourRepository.existsByTitle(title);
    }

    @Override
    public TourImage createTourImage(Long tourId, TourImageDTO tourImageDTO) throws Exception {
        Tour existingTour = tourRepository.findById(tourId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với id: " + tourId));

        long size = tourImageRepository.countByTourTourId(tourId);
        if (size >= 5) {
            throw new InvalidParamException("Số lượng hình ảnh phải <= 5");
        }

        TourImage newTourImage = TourImage.builder()
                .tour(existingTour)
                .imageUrl(tourImageDTO.getImageUrl())
                .build();

        return tourImageRepository.save(newTourImage);
    }

    @Override
    public List<Review> getReviewsByTour(Long tourId) {
        return reviewRepository.findByTourTourId(tourId);
    }

    @Override
    public List<Review> getReviewsByUserAndTour(Long userId, Long tourId) {
        return reviewRepository.findByTourTourIdAndUserUserId(tourId, userId);
    }

    private List<TourImage> createTourImagesFromDTO(Tour tour, List<String> imageUrls) throws InvalidParamException {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ArrayList<>();
        }
        if (imageUrls.size() > 5) {
            throw new InvalidParamException("Số lượng hình ảnh phải <= 5");
        }
        return imageUrls.stream()
                .map(imageUrl -> {
                    TourImage tourImage = new TourImage();
                    tourImage.setTour(tour);
                    String fileName = imageUrl.replace("http://localhost:8088/api/v1/tours/images/", "");
                    tourImage.setImageUrl(fileName);
                    return tourImage;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<TourImage> updateTourImages(Long tourId, List<TourImageDTO> tourImageDTOs) throws Exception {
        Tour existingTour = tourRepository.findById(tourId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy tour với id: " + tourId));

        if (tourImageDTOs != null && tourImageDTOs.size() > 5) {
            throw new InvalidParamException("Số lượng hình ảnh phải <= 5");
        }

        existingTour.getTourImages().clear();

        if (tourImageDTOs != null && !tourImageDTOs.isEmpty()) {
            for (TourImageDTO dto : tourImageDTOs) {
                TourImage newTourImage = new TourImage();
                newTourImage.setTour(existingTour);
                newTourImage.setImageUrl(dto.getImageUrl());
                existingTour.getTourImages().add(newTourImage);
            }
        }

        Tour savedTour = tourRepository.save(existingTour);
        return savedTour.getTourImages();
    }

    @Override
    public List<BookedTourResponse> getBookedToursByUserId(Long userId, BookingStatus status) {
        List<Booking> bookings = (status == null)
                ? bookingService.findBookingsByUserId(userId)
                : bookingService.findBookingsByUserIdAndStatus(userId, status);

        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        return bookings.stream()
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .map(booking -> {
                    try {
                        // Tính countStar
                        Map<Integer, Long> countStar = new HashMap<>();
                        for (int i = 1; i <= 5; i++) {
                            Long count = reviewRepository.countByTourTourIdAndRating(booking.getTour().getTourId(), i);
                            countStar.put(i, count);
                        }
                        return BookedTourResponse.fromBooking(booking, objectMapper, countStar);
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi khi chuyển đổi booking: " + e.getMessage());
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }   

    @Override
    public List<TopBookedTourResponse> getTopBookedTours() {
        PageRequest pageable = PageRequest.of(0, 4);
        List<Tour> topTours = tourRepository.findTopBookedTours(pageable);

        return topTours.stream()
                .map(tour -> {
                    Integer totalBookedTickets = tourRepository.getTotalBookedTicketsByTourId(tour.getTourId());
                    int availableSlots = Math.max(0, tour.getQuantity() - (totalBookedTickets != null ? totalBookedTickets : 0));
                    Float averageRating = reviewRepository.findAverageRatingByTourId(tour.getTourId()).orElse(0.0f);

                    // Tính countStar
                    Map<Integer, Long> countStar = new HashMap<>();
                    for (int i = 1; i <= 5; i++) {
                        Long count = reviewRepository.countByTourTourIdAndRating(tour.getTourId(), i);
                        countStar.put(i, count);
                    }

                    return TopBookedTourResponse.fromTour(tour, availableSlots, averageRating, countStar);
                })
                .collect(Collectors.toList());
    }
    @Override
    public List<String> getAllDeparturePoints() {
        return tourRepository.findAllDeparturePoints();
    }

    @Override
    public List<String> getAllDestinations() {
        return tourRepository.findAllDestinations();
    }
}
