package com.project.booktour.repositories;

import com.project.booktour.models.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BannerRepository extends JpaRepository<Banner, Integer> {
    @Query("SELECT b FROM Banner b WHERE b.title LIKE %:keyword% AND b.isActive = true")
    Page<Banner> findAllActive(String keyword, Pageable pageable);
}