package com.project.booktour.services;

import com.project.booktour.dtos.BannerDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.Banner;
import com.project.booktour.repositories.BannerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BannerService implements IBannerService {

    private final BannerRepository bannerRepository;
    private static final String BANNER_UPLOAD_DIR = "uploads/banners/";
    private static final String DEFAULT_BANNER_IMAGE = "default-banner.jpg"; // Chỉ lưu tên file

    @Override
    @Transactional
    public Banner createBanner(BannerDTO bannerDTO, MultipartFile image) throws Exception {
        Banner banner = Banner.builder()
                .title(bannerDTO.getTitle())
                .imageUrl(DEFAULT_BANNER_IMAGE) // Chỉ lưu tên file, không lưu full path
                .link(bannerDTO.getLink())
                .position(bannerDTO.getPosition())
                .priority(bannerDTO.getPriority() != null ? bannerDTO.getPriority() : 0)
                .isActive(bannerDTO.getIsActive() != null ? bannerDTO.getIsActive() : true)
                .startDate(bannerDTO.getStartDate())
                .endDate(bannerDTO.getEndDate())
                .build();

        // Save banner trước để có ID
        Banner savedBanner = bannerRepository.save(banner);

        // Nếu có image thì upload và update
        if (image != null && !image.isEmpty()) {
            String fileName = storeFile(image);
            if (fileName != null) {
                savedBanner.setImageUrl(fileName); // Chỉ lưu tên file
                savedBanner = bannerRepository.save(savedBanner);
            }
        }

        return savedBanner;
    }

    @Override
    public Page<Banner> getAllBanners(String keyword, Pageable pageable) {
        return bannerRepository.findAllActive(keyword, pageable);
    }

    @Override
    public Banner getBannerById(Integer id) throws Exception {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy banner với id: " + id));
    }

    @Override
    @Transactional
    public Banner updateBanner(Integer id, BannerDTO bannerDTO) throws Exception {
        Banner existingBanner = bannerRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy banner với id: " + id));

        // Chỉ update các field cần thiết, KHÔNG touch imageUrl
        existingBanner.setTitle(bannerDTO.getTitle());

        if (bannerDTO.getLink() != null) {
            existingBanner.setLink(bannerDTO.getLink());
        }
        if (bannerDTO.getPosition() != null) {
            existingBanner.setPosition(bannerDTO.getPosition());
        }
        if (bannerDTO.getPriority() != null) {
            existingBanner.setPriority(bannerDTO.getPriority());
        }
        if (bannerDTO.getIsActive() != null) {
            existingBanner.setIsActive(bannerDTO.getIsActive());
        }
        if (bannerDTO.getStartDate() != null) {
            existingBanner.setStartDate(bannerDTO.getStartDate());
        }
        if (bannerDTO.getEndDate() != null) {
            existingBanner.setEndDate(bannerDTO.getEndDate());
        }

        return bannerRepository.save(existingBanner);
    }

    @Override
    @Transactional
    public Banner updateBannerImage(Integer id, MultipartFile image) throws Exception {
        Banner existingBanner = bannerRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy banner với id: " + id));

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        // Xóa ảnh cũ nếu có (trừ default image)
        String oldImageUrl = existingBanner.getImageUrl();
        if (oldImageUrl != null && !oldImageUrl.equals(DEFAULT_BANNER_IMAGE)) {
            deleteOldImage(oldImageUrl);
        }

        // Upload ảnh mới
        String fileName = storeFile(image);
        if (fileName != null) {
            existingBanner.setImageUrl(fileName); // Chỉ lưu tên file
        }

        return bannerRepository.save(existingBanner);
    }

    @Override
    @Transactional
    public void deleteBanner(Integer id) throws Exception {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy banner với id: " + id));

        // Xóa ảnh trước khi xóa banner
        String imageUrl = banner.getImageUrl();
        if (imageUrl != null && !imageUrl.equals(DEFAULT_BANNER_IMAGE)) {
            deleteOldImage(imageUrl);
        }

        bannerRepository.delete(banner);
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (file.getSize() == 0) {
            return null;
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh banner không được vượt quá 5MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là hình ảnh (jpg, png, gif, etc.)");
        }

        // Kiểm tra tên file
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        try {
            String fileName = UUID.randomUUID().toString() + "-" + originalFilename;
            Path uploadDir = Paths.get(BANNER_UPLOAD_DIR);

            // Tạo thư mục nếu chưa tồn tại
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path destination = Paths.get(uploadDir.toString(), fileName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new IOException("Lỗi khi lưu file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi không xác định khi lưu file: " + e.getMessage(), e);
        }
    }

    private void deleteOldImage(String fileName) {
        try {
            // fileName đã là tên file thuần túy
            Path imagePath = Paths.get(BANNER_UPLOAD_DIR + fileName);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
            }
        } catch (Exception e) {
            // Log error but don't throw exception
            System.err.println("Không thể xóa ảnh cũ: " + e.getMessage());
        }
    }
}