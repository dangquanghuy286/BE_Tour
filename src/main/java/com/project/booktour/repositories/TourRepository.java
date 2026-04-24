package com.project.booktour.repositories;

import com.project.booktour.models.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    boolean existsByTitle(String title);

    Long countByAvailabilityTrue();

    @Query("SELECT t, AVG(r.rating) as avgRating, " +
            "(SELECT ti.imageUrl FROM TourImage ti WHERE ti.tour = t ORDER BY ti.id ASC LIMIT 1), " +
            "(SELECT COALESCE(SUM(b.numAdults + b.numChildren), 0) FROM Booking b WHERE b.tour = t) as totalBookedTickets " +
            "FROM Tour t LEFT JOIN t.reviews r " +
            "WHERE (:priceMin IS NULL OR t.priceAdult >= :priceMin) " +
            "AND (:priceMax IS NULL OR t.priceAdult <= :priceMax) " +
            "AND (:region IS NULL OR UPPER(t.region) = UPPER(:region)) " +
            "AND (:duration IS NULL OR t.duration = :duration) " +
            "AND (:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:departurePoint IS NULL OR t.departurePoint = :departurePoint) " +
            "AND (:destination IS NULL OR t.destination = :destination) " +
            "GROUP BY t " +
            "HAVING (:starRating IS NULL OR AVG(r.rating) = :starRating)")
    Page<Object[]> findAllWithFilters(PageRequest pageRequest,
                                      @Param("priceMin") Double priceMin,
                                      @Param("priceMax") Double priceMax,
                                      @Param("region") String region,
                                      @Param("starRating") Float starRating,
                                      @Param("duration") String duration,
                                      @Param("title") String title,
                                      @Param("departurePoint") String departurePoint,
                                      @Param("destination") String destination);

    @Query("SELECT t FROM Tour t LEFT JOIN Booking b ON t.tourId = b.tour.tourId " +
            "GROUP BY t ORDER BY COUNT(b) DESC")
    List<Tour> findTopBookedTours(Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.numAdults + b.numChildren), 0) FROM Booking b WHERE b.tour.tourId = :tourId")
    Integer getTotalBookedTicketsByTourId(@Param("tourId") Long tourId);

    // Thêm hai truy vấn mới
    @Query("SELECT DISTINCT t.departurePoint FROM Tour t WHERE t.departurePoint IS NOT NULL ORDER BY t.departurePoint")
    List<String> findAllDeparturePoints();

    @Query("SELECT DISTINCT t.destination FROM Tour t WHERE t.destination IS NOT NULL ORDER BY t.destination")
    List<String> findAllDestinations();
}