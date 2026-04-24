package com.project.booktour.services.review;

import com.project.booktour.dtos.ReviewDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.Review;
import com.project.booktour.responses.reviewsreponse.ReviewResponse;

import java.util.List;

public interface IReviewService {
    List<Review> getAllReviews();
    Review createReview(ReviewDTO reviewDTO) throws Exception;
    List<Review> getReviewsByTour(Long tourId);
    List<Review> getReviewsByUserAndTour(Long userId, Long tourId);
    List<ReviewResponse> getReviewListByTour(Long tourId) throws DataNotFoundException;
    void updateReview(Long reviewId, ReviewDTO reviewDTO) throws Exception;
    void deleteReview(Long reviewId, Long userId) throws Exception;
}