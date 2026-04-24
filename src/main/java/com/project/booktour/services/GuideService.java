package com.project.booktour.services;

import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.models.Guide;
import com.project.booktour.repositories.GuideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class GuideService {

    @Autowired
    private GuideRepository guideRepository;

    private static final String GUIDE_IMAGE_UPLOAD_DIR = "Uploads/guide_images/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // Create a new guide
    public Guide createGuide(Guide guide) throws InvalidParamException {
        validateGuideData(guide);
        try {
            return guideRepository.save(guide);
        } catch (Exception e) {
            throw new RuntimeException("Error saving guide: " + e.getMessage(), e);
        }
    }

    // Get all guides with pagination
    public Page<Guide> getAllGuides(Pageable pageable) {
        try {
            return guideRepository.findAll(pageable);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving guides: " + e.getMessage(), e);
        }
    }

    // Get guide by ID
    public Optional<Guide> getGuideById(Integer id) throws InvalidParamException {
        if (id == null || id <= 0) {
            throw new InvalidParamException("Invalid guide ID");
        }
        try {
            return guideRepository.findById(id);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving guide with ID " + id + ": " + e.getMessage(), e);
        }
    }

    // Update guide
    public Guide updateGuide(Integer id, Guide guideDetails) throws DataNotFoundException, InvalidParamException {
        if (id == null || id <= 0) {
            throw new InvalidParamException("Invalid guide ID");
        }
        validateGuideData(guideDetails);
        try {
            return guideRepository.findById(id)
                    .map(existingGuide -> {
                        existingGuide.setFullName(guideDetails.getFullName());
                        existingGuide.setAge(guideDetails.getAge());
                        existingGuide.setGender(guideDetails.getGender());
                        existingGuide.setPhoneNumber(guideDetails.getPhoneNumber());
                        existingGuide.setDatabaseLink(guideDetails.getDatabaseLink());
                        existingGuide.setGmailLink(guideDetails.getGmailLink());
                        if (guideDetails.getPhoto() != null && !guideDetails.getPhoto().isEmpty()) {
                            existingGuide.setPhoto(guideDetails.getPhoto());
                        }
                        existingGuide.setIsActive(guideDetails.getIsActive());
                        return guideRepository.save(existingGuide);
                    })
                    .orElseThrow(() -> new DataNotFoundException("Guide not found with ID: " + id));
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error updating guide: " + e.getMessage(), e);
        }
    }

    // Delete guide
    public void deleteGuide(Integer id) throws DataNotFoundException, InvalidParamException {
        if (id == null || id <= 0) {
            throw new InvalidParamException("Invalid guide ID");
        }
        try {
            if (guideRepository.existsById(id)) {
                guideRepository.deleteById(id);
            } else {
                throw new DataNotFoundException("Guide not found with ID: " + id);
            }
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting guide: " + e.getMessage(), e);
        }
    }

    // Upload photo for guide
    public Guide updateGuidePhoto(Integer guideId, MultipartFile photo) throws DataNotFoundException, InvalidParamException, IOException {
        if (guideId == null || guideId <= 0) {
            throw new InvalidParamException("Invalid guide ID");
        }
        if (photo == null || photo.isEmpty()) {
            throw new InvalidParamException("Photo file cannot be empty");
        }
        try {
            Guide existingGuide = guideRepository.findById(guideId)
                    .orElseThrow(() -> new DataNotFoundException("Guide not found with ID: " + guideId));
            String fileName = storeFile(photo);
            if (fileName != null) {
                existingGuide.setPhoto(fileName);
            }
            return guideRepository.save(existingGuide);
        } catch (DataNotFoundException | InvalidParamException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error updating guide photo: " + e.getMessage(), e);
        }
    }

    // Validate guide data
    private void validateGuideData(Guide guide) throws InvalidParamException {
        if (guide == null) {
            throw new InvalidParamException("Guide data cannot be null");
        }
        if (guide.getFullName() == null || guide.getFullName().trim().isEmpty()) {
            throw new InvalidParamException("Guide name cannot be empty");
        }
        if (guide.getFullName().length() > 50) {
            throw new InvalidParamException("Guide name cannot exceed 50 characters");
        }
        if (guide.getAge() == null || guide.getAge() < 18 || guide.getAge() > 100) {
            throw new InvalidParamException("Guide age must be between 18 and 100");
        }
        if (guide.getGender() == null) {
            throw new InvalidParamException("Guide gender cannot be null");
        }
        if (guide.getPhoneNumber() == null || guide.getPhoneNumber().trim().isEmpty()) {
            throw new InvalidParamException("Guide phone number cannot be empty");
        }
        if (guide.getPhoneNumber().length() > 15) {
            throw new InvalidParamException("Guide phone number cannot exceed 15 characters");
        }
    }

    // Store file with validation
    private String storeFile(MultipartFile file) throws IOException, InvalidParamException {
        if (file.isEmpty()) {
            throw new InvalidParamException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidParamException("Photo size cannot exceed 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidParamException("File must be an image (jpg, png, gif, etc.)");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidParamException("Invalid file name");
        }
        try {
            String fileName = UUID.randomUUID() + "-" + originalFilename;
            Path uploadPath = Paths.get(GUIDE_IMAGE_UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new IOException("Error storing file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unknown error storing file: " + e.getMessage(), e);
        }
    }
}