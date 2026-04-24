package com.project.booktour.controllers;

import com.project.booktour.dtos.GuideDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.models.Guide;
import com.project.booktour.responses.ErrorResponse;
import com.project.booktour.responses.GuideListResponse;
import com.project.booktour.services.GuideService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/guides")
public class GuideController {

    @Autowired
    private GuideService guideService;

    @Autowired
    private ModelMapper modelMapper;

    // Convert Guide to GuideDTO with full photo URL
    private GuideDTO convertToGuideDTO(Guide guide) {
        GuideDTO guideDTO = modelMapper.map(guide, GuideDTO.class);
        if (guide.getPhoto() != null && !guide.getPhoto().isEmpty()) {
            String fullPhotoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/guides/images/")
                    .path(guide.getPhoto())
                    .toUriString();
            guideDTO.setPhoto(fullPhotoUrl);
        }
        return guideDTO;
    }

    // Create a new guide
    @PostMapping
    public ResponseEntity<?> createGuide(@RequestBody GuideDTO guideDTO) {
        try {
            if (guideDTO == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Bad Request", "Guide data cannot be null", 400));
            }
            Guide guide = modelMapper.map(guideDTO, Guide.class);
            Guide savedGuide = guideService.createGuide(guide);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToGuideDTO(savedGuide));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Error creating guide: " + e.getMessage(), 500));
        }
    }

    // Get all guides with pagination and sorting
    @GetMapping
    public ResponseEntity<?> getAllGuides(@RequestParam Map<String, String> params) {
        try {
            int page = parseIntParam(params.getOrDefault("page", "0"), "page");
            int limit = parseIntParam(params.getOrDefault("limit", "10"), "limit");
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "Page must be >= 0", 400));
            }
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "Limit must be > 0 and <= 100", 400));
            }
            String sortBy = params.getOrDefault("sortBy", "createdAt");
            String sortDir = params.getOrDefault("sortDir", "desc");
            List<String> validSortFields = List.of("createdAt", "fullName", "age");
            if (!validSortFields.contains(sortBy)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter",
                                "sortBy is invalid. Allowed: " + validSortFields, 400));
            }
            if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "sortDir must be 'asc' or 'desc'", 400));
            }
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
            PageRequest pageRequest = PageRequest.of(page, limit, sort);
            Page<Guide> guidePage = guideService.getAllGuides(pageRequest);
            List<GuideDTO> guideDTOs = guidePage.getContent().stream()
                    .map(this::convertToGuideDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new GuideListResponse(guideDTOs, guidePage.getTotalPages(), guidePage.getTotalElements()));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", "Invalid number parameter: " + e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Error retrieving guides: " + e.getMessage(), 500));
        }
    }

    // Get guide by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getGuideById(@PathVariable Integer id) {
        try {
            Optional<Guide> guide = guideService.getGuideById(id);
            if (guide.isPresent()) {
                return ResponseEntity.ok(convertToGuideDTO(guide.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Guide not found with ID: " + id, 404));
            }
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Error retrieving guide: " + e.getMessage(), 500));
        }
    }

    // Update guide
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGuide(@PathVariable Integer id, @RequestBody GuideDTO guideDTO) {
        try {
            if (guideDTO == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Bad Request", "Guide data cannot be null", 400));
            }
            Guide guideDetails = modelMapper.map(guideDTO, Guide.class);
            Guide updatedGuide = guideService.updateGuide(id, guideDetails);
            return ResponseEntity.ok(convertToGuideDTO(updatedGuide));
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage(), 404));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Error updating guide: " + e.getMessage(), 500));
        }
    }

    // Delete guide
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGuide(@PathVariable Integer id) {
        try {
            guideService.deleteGuide(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Guide deleted successfully");
            return ResponseEntity.ok(response);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage(), 404));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Error deleting guide: " + e.getMessage(), 500));
        }
    }

    // Upload photo for guide
    @PutMapping("/{id}/photo")
    public ResponseEntity<?> updateGuidePhoto(@PathVariable Integer id, @RequestParam("photo") MultipartFile photo) {
        try {
            Guide updatedGuide = guideService.updateGuidePhoto(id, photo);
            return ResponseEntity.ok(convertToGuideDTO(updatedGuide));
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage(), 404));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("File Error", "Error processing file: " + e.getMessage(), 500));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error", "Error updating guide photo: " + e.getMessage(), 500));
        }
    }

    // View guide photo
    @GetMapping("/images/{photoName}")
    public ResponseEntity<?> viewGuidePhoto(@PathVariable String photoName) {
        try {
            if (photoName == null || photoName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "Invalid photo name", 400));
            }
            Path photoPath = Paths.get("Uploads/guide_images/" + photoName);
            UrlResource resource = new UrlResource(photoPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new UrlResource("https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", "Photo not found: " + e.getMessage(), 404));
        }
    }

    // Helper method to parse integer parameters
    private int parseIntParam(String param, String paramName) throws NumberFormatException {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid parameter '" + paramName + "': " + param);
        }
    }
}