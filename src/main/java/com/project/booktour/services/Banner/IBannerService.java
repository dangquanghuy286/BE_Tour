package com.project.booktour.services;

import com.project.booktour.dtos.BannerDTO;
import com.project.booktour.models.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IBannerService {
    Banner createBanner(BannerDTO bannerDTO, MultipartFile image) throws Exception;
    Page<Banner> getAllBanners(String keyword, Pageable pageable);
    Banner getBannerById(Integer id) throws Exception;
    Banner updateBanner(Integer id, BannerDTO bannerDTO) throws Exception; // Chỉ nhận 2 tham số
    void deleteBanner(Integer id) throws Exception;
    Banner updateBannerImage(Integer id, MultipartFile image) throws Exception; // Giữ nguyên để upload ảnh riêng
}