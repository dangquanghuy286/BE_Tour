package com.project.booktour.repositories;

import com.project.booktour.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTourTourId(Long tourId);
    List<Review> findByTourTourIdAndUserUserId(Long tourId, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tour.tourId = :tourId")
    Optional<Float> findAverageRatingByTourId(Long tourId);

    Integer countByTourTourId(Long tourId);
    @Query("SELECT COUNT(r) FROM Review r WHERE r.tour.tourId = :tourId AND r.rating = :rating")
    Long countByTourTourIdAndRating(Long tourId, int rating);

    boolean existsByUserUserIdAndTourTourId(Long userId, Long tourId);
}