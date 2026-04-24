package com.project.booktour.controllers;

import com.project.booktour.dtos.BannerDTO;
import com.project.booktour.exceptions.CustomException;
import com.project.booktour.models.Banner;
import com.project.booktour.responses.ErrorResponse;
import com.project.booktour.services.IBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/banners")
@RequiredArgsConstructor
public class BannerController {

    private final IBannerService bannerService;

    // Chuyển đổi Banner thành DTO với URL ảnh đầy đủ
    private Banner convertToBannerWithFullImageUrl(Banner banner) {
        if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty() && !banner.getImageUrl().startsWith("http")) {
            // Nếu là tên file (không phải URL đầy đủ)
            String fileName = banner.getImageUrl().contains("/") ?
                    banner.getImageUrl().substring(banner.getImageUrl().lastIndexOf("/") + 1) :
                    banner.getImageUrl();

            String fullImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/banners/images/")
                    .path(fileName)
                    .toUriString();

            // Tạo bản copy để không thay đổi entity gốc
            Banner bannerCopy = Banner.builder()
                    .id(banner.getId())
                    .title(banner.getTitle())
                    .imageUrl(fullImageUrl)
                    .link(banner.getLink())
                    .position(banner.getPosition())
                    .priority(banner.getPriority())
                    .isActive(banner.getIsActive())
                    .startDate(banner.getStartDate())
                    .endDate(banner.getEndDate())
                    .createdAt(banner.getCreatedAt())
                    .updatedAt(banner.getUpdatedAt())
                    .build();

            return bannerCopy;
        }
        return banner;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createBanner(
            @Valid @RequestBody BannerDTO bannerDTO,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream().map(FieldError::getDefaultMessage).toList();
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PARAM", String.join("; ", errorMessages), HttpStatus.BAD_REQUEST.value()));
            }

            Banner banner = bannerService.createBanner(bannerDTO, null);
            return ResponseEntity.ok(convertToBannerWithFullImageUrl(banner));
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getAllBanners(
            @RequestParam(defaultValue = "", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            PageRequest pageRequest = PageRequest.of(page, limit, Sort.by("id").ascending());
            Page<Banner> bannerPage = bannerService.getAllBanners(keyword, pageRequest);

            // Convert tất cả banner trong page
            Page<Banner> convertedPage = bannerPage.map(this::convertToBannerWithFullImageUrl);

            return ResponseEntity.ok(convertedPage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBannerById(@PathVariable Integer id) {
        try {
            Banner banner = bannerService.getBannerById(id);
            return ResponseEntity.ok(convertToBannerWithFullImageUrl(banner));
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateBanner(
            @PathVariable Integer id,
            @Valid @RequestBody BannerDTO bannerDTO,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream().map(FieldError::getDefaultMessage).toList();
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PARAM", String.join("; ", errorMessages), HttpStatus.BAD_REQUEST.value()));
            }

            Banner updatedBanner = bannerService.updateBanner(id, bannerDTO);
            return ResponseEntity.ok(convertToBannerWithFullImageUrl(updatedBanner));
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{id}/upload-image")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> uploadBannerImage(
            @PathVariable Integer id,
            @RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PARAM", "File ảnh không được để trống", HttpStatus.BAD_REQUEST.value()));
            }

            Banner updatedBanner = bannerService.updateBannerImage(id, image);
            return ResponseEntity.ok(convertToBannerWithFullImageUrl(updatedBanner));
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteBanner(@PathVariable Integer id) {
        try {
            bannerService.deleteBanner(id);
            return ResponseEntity.ok("Xóa banner thành công.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getErrorCode(), e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("UNKNOWN_ERROR", "Đã xảy ra lỗi không xác định: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<?> viewBannerImage(@PathVariable String imageName) {
        try {
            if (imageName == null || imageName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_PARAMETER", "Tên ảnh không hợp lệ", HttpStatus.BAD_REQUEST.value()));
            }

            Path imagePath = Paths.get("uploads/banners/" + imageName);
            UrlResource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                // Trả về ảnh mặc định
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new UrlResource("https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("IMAGE_NOT_FOUND", "Không tìm thấy ảnh banner: " + e.getMessage(), HttpStatus.NOT_FOUND.value()));
        }
    }
}