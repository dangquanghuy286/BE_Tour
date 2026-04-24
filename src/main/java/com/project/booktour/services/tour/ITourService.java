package com.project.booktour.services.tour;

import com.project.booktour.dtos.TourDTO;
import com.project.booktour.dtos.TourImageDTO;
import com.project.booktour.models.*;
import com.project.booktour.responses.toursreponse.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface ITourService {
    Tour createTour(TourDTO tourDTO) throws Exception;
    Tour getTourById(Long tourId) throws Exception;
    TourResponse getTourDetails(Long id) throws Exception;
    Page<SimplifiedTourResponse> getAllTours(PageRequest pageRequest, Double priceMin, Double priceMax, String region, Float starRating, String duration, String title, String tag, String departurePoint, String destination);
    Tour updateTour(Long id, TourDTO tourDTO) throws Exception;
    void deleteTour(Long id);
    boolean existsByTitle(String title);
    TourImage createTourImage(Long tourId, TourImageDTO tourImageDTO) throws Exception;
    List<Review> getReviewsByTour(Long tourId);
    List<Review> getReviewsByUserAndTour(Long userId, Long tourId);
    List<TourImage> updateTourImages(Long tourId, List<TourImageDTO> tourImageDTOs) throws Exception;
    List<BookedTourResponse> getBookedToursByUserId(Long userId, BookingStatus status);
    List<TopBookedTourResponse> getTopBookedTours();

    // Thêm hai phương thức mới
    List<String> getAllDeparturePoints();
    List<String> getAllDestinations();
}