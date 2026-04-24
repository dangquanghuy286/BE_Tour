package com.project.booktour.repositories;

import com.project.booktour.models.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourImageRepository extends JpaRepository<TourImage, Long> {
    // Tìm danh sách TourImage theo tourId (đổi tên phương thức để khớp với tour.tourId)
    List<TourImage> findByTourTourId(Long tourId);

    // Đếm số lượng TourImage của một tour
    long countByTourTourId(Long tourId);

    // Xóa tất cả TourImage của một tour
    @Modifying
    @Query("DELETE FROM TourImage ti WHERE ti.tour.tourId = :tourId")
    void deleteByTourTourId(Long tourId);
}