package com.project.booktour.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.booktour.dtos.TourDTO;
import com.project.booktour.dtos.TourImageDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.models.*;
import com.project.booktour.responses.toursreponse.*;
import com.project.booktour.services.booking.IBookingService;
import com.project.booktour.services.tour.ITourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/tours")
@RequiredArgsConstructor
public class TourController {
    private static final Logger logger = LoggerFactory.getLogger(TourController.class);
    private final ITourService tourService;

    @GetMapping("/user/{user_id}")
    public ResponseEntity<?> getBookedTours(
            @Valid @PathVariable("user_id") Long userId,
            @RequestParam(value = "booking_status", required = false) BookingStatus status) {
        try {
            List<BookedTourResponse> bookedTours = tourService.getBookedToursByUserId(userId, status);
            if (bookedTours.isEmpty()) {
                return ResponseEntity.ok("Không tìm thấy tour nào đã đặt cho user này.");
            }
            return ResponseEntity.ok(bookedTours);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Đã xảy ra lỗi khi lấy danh sách tour đã đặt: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getTours(@RequestParam Map<String, String> params) {
        try {
            logger.info("Received request to get tours with params: {}", params);

            int page = Integer.parseInt(params.getOrDefault("page", "0"));
            int limit = Integer.parseInt(params.getOrDefault("limit", "10"));

            if (page < 0 || limit <= 0) {
                return ResponseEntity.badRequest().body("Page must be >= 0 and limit must be > 0");
            }

            String title = params.get("title");
            String sortBy = params.getOrDefault("sortBy", "createdAt");
            String sortDir = params.getOrDefault("sortDir", "desc");
            String tag = params.get("tag");
            String departurePoint = params.get("departurePoint");
            String destination = params.get("destination"); // Thêm destination

            List<String> validSortFields = List.of("createdAt", "priceAdult");
            if (!sortBy.equals("createdAt") && !sortBy.equals("priceAdult")) {
                return ResponseEntity.badRequest().body("Invalid sortBy field: " + sortBy + ". Must be 'createdAt' or 'priceAdult'");
            }

            if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest().body("sortDir must be 'asc' or 'desc'");
            }

            if (tag != null && !List.of("Economy", "Standard", "Premium").contains(tag)) {
                return ResponseEntity.badRequest().body("Invalid tag: " + tag + ". Must be 'Economy', 'Standard', or 'Premium'");
            }

            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
            PageRequest pageRequest = PageRequest.of(page, limit, sort);
            Double priceMin = params.containsKey("priceMin") ? Double.parseDouble(params.get("priceMin")) : null;
            Double priceMax = params.containsKey("priceMax") ? Double.parseDouble(params.get("priceMax")) : null;
            String region = params.get("region");
            Float starRating = params.containsKey("starRating") ? Float.parseFloat(params.get("starRating")) : null;
            String duration = params.get("duration");

            if (priceMin != null && priceMin < 0) {
                return ResponseEntity.badRequest().body("priceMin must be >= 0");
            }
            if (priceMax != null && priceMax < 0) {
                return ResponseEntity.badRequest().body("priceMax must be >= 0");
            }
            if (priceMin != null && priceMax != null && priceMin > priceMax) {
                return ResponseEntity.badRequest().body("priceMin must be <= priceMax");
            }
            if (starRating != null && (starRating < 0 || starRating > 5)) {
                return ResponseEntity.badRequest().body("starRating must be between 0 and 5");
            }
            if (region != null) {
                try {
                    Region.valueOf(region.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Invalid region: " + region);
                }
            }

            Page<SimplifiedTourResponse> tourPage = tourService.getAllTours(pageRequest, priceMin, priceMax, region, starRating, duration, title, tag, departurePoint, destination);
            int totalPages = tourPage.getTotalPages();
            List<SimplifiedTourResponse> tours = tourPage.getContent();
            return ResponseEntity.ok(TourListResponse.builder()
                    .tours(tours)
                    .totalPages(totalPages)
                    .build());
        } catch (NumberFormatException e) {
            logger.error("Invalid number format for parameters: {}", params, e);
            return ResponseEntity.badRequest().body("Invalid number format for parameters: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching tours: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getTourResponseById(@PathVariable("id") Long tourId) {
        try {
            TourResponse tourResponse = tourService.getTourDetails(tourId);
            return ResponseEntity.ok(tourResponse);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Tour not found with id: " + tourId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching tour with id: " + tourId);
        }
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<?> viewImage(@PathVariable String imageName) {
        try {
            Path imagePath = Paths.get("uploads/" + imageName);
            UrlResource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new UrlResource(Paths.get("uploads/notfound.jpeg").toUri()));
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("")
    public ResponseEntity<?> createTour(@Valid @RequestBody TourDTO tourDTO, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }
            Tour newTour = tourService.createTour(tourDTO);
            TourResponse tourResponse = tourService.getTourDetails(newTour.getTourId());
            return ResponseEntity.status(HttpStatus.CREATED).body(tourResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create tour: " + e.getMessage());
        }
    }

    @PostMapping(value = "/uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @PathVariable("id") Long tourId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body("No files uploaded");
            }

            Tour existingTour = tourService.getTourById(tourId);
            List<String> imageUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.getSize() == 0) {
                    continue;
                }
                if (file.getSize() > 10 * 1024 * 1024) {
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                            .body("File is too large! Maximum size is 10MB");
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body("File must be an image");
                }

                String fileName = storeFile(file);
                TourImage tourImage = tourService.createTourImage(
                        existingTour.getTourId(),
                        TourImageDTO.builder().imageUrl(fileName).build()
                );
                String imageUrl = "http://localhost:8088/api/v1/tours/images/" + fileName;
                imageUrls.add(imageUrl);
            }

            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to upload images: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/images")
    public ResponseEntity<?> updateTourImages(
            @PathVariable("id") Long tourId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body("No files uploaded");
            }

            if (files.size() > 5) {
                return ResponseEntity.badRequest().body("Number of images must be <= 5");
            }

            Tour existingTour = tourService.getTourById(tourId);
            List<TourImageDTO> tourImageDTOs = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.getSize() == 0) {
                    continue;
                }
                if (file.getSize() > 10 * 1024 * 1024) {
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                            .body("File is too large! Maximum size is 10MB");
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body("File must be an image");
                }

                String fileName = storeFile(file);
                tourImageDTOs.add(TourImageDTO.builder().imageUrl(fileName).build());
            }

            List<TourImage> updatedImages = tourService.updateTourImages(tourId, tourImageDTOs);
            List<String> imageUrls = updatedImages.stream()
                    .map(image -> "http://localhost:8088/api/v1/tours/images/" + image.getImageUrl())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to update tour images: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTour(@PathVariable Long id) {
        try {
            tourService.deleteTour(id);
            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Xóa tour thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Không tìm thấy tour",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Lỗi khi xóa tour",
                    "details", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTour(
            @PathVariable("id") Long id,
            @Valid @RequestBody TourDTO tourDTO,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errorMessages = result.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(errorMessages);
            }
            Tour updatedTour = tourService.updateTour(id, tourDTO);
            if (updatedTour == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Tour not found with id: " + id);
            }
            TourResponse tourResponse = tourService.getTourDetails(updatedTour.getTourId());
            return ResponseEntity.ok(tourResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to update tour: " + e.getMessage());
        }
    }

    private String storeFile(MultipartFile file) throws IOException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID().toString() + "-" + filename;
        if (!isImage(file)) {
            throw new IllegalArgumentException("The file is not an image.");
        }
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path destination = Paths.get(uploadDir.toString(), uniqueFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return uniqueFilename;
    }

    private boolean isImage(MultipartFile file) throws IOException {
        Tika tika = new Tika();
        String mimeType = tika.detect(file.getInputStream());
        return mimeType.startsWith("image/");
    }

    @GetMapping("/top-booked")
    public ResponseEntity<?> getTopBookedTours() {
        try {
            List<TopBookedTourResponse> topTours = tourService.getTopBookedTours();
            return ResponseEntity.ok(topTours);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách tour được đặt nhiều nhất: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<?> getTourImages(@PathVariable("id") Long tourId) {
        try {
            Tour tour = tourService.getTourById(tourId);
            List<TourImage> tourImages = tour.getTourImages();
            List<String> imageUrls = tourImages.stream()
                    .map(image -> "http://localhost:8088/api/v1/tours/images/" + image.getImageUrl())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(imageUrls);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Tour not found with id: " + tourId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch tour images: " + e.getMessage());
        }
    }
    @GetMapping("/locations")
    public ResponseEntity<?> getDepartureAndDestinationPoints() {
        try {
            // Lấy danh sách điểm đi và điểm đến từ service
            List<String> departurePoints = tourService.getAllDeparturePoints();
            List<String> destinations = tourService.getAllDestinations();

            // Tạo response object
            Map<String, List<String>> response = new HashMap<>();
            response.put("departurePoints", departurePoints);
            response.put("destinations", destinations);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error while fetching departure and destination points: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách điểm đi và điểm đến: " + e.getMessage());
        }
    }
}
