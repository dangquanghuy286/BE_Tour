package com.project.booktour.controllers;

import com.project.booktour.dtos.ReviewDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.exceptions.UnauthorizedException;
import com.project.booktour.models.Review;
import com.project.booktour.models.User;
import com.project.booktour.responses.reviewsreponse.ReviewResponse;
import com.project.booktour.services.review.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        List<ReviewResponse> reviewResponses = reviewService.getAllReviews().stream()
                .map(ReviewResponse::fromReview)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviewResponses);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> insertReview(@Valid @RequestBody ReviewDTO reviewDTO) {
        try {
            User loginUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!loginUser.getUserId().equals(reviewDTO.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn không thể đánh giá thay cho người dùng khác");
            }
            Review review = reviewService.createReview(reviewDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Đánh giá đã được tạo thành công cho tour ID: " + review.getTour().getTourId());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (InvalidParamException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi tạo đánh giá: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> updateReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody ReviewDTO reviewDTO
    ) {
        try {
            User loginUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!loginUser.getUserId().equals(reviewDTO.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn không thể cập nhật đánh giá của người dùng khác");
            }
            reviewService.updateReview(reviewId, reviewDTO);
            return ResponseEntity.ok("Đánh giá đã được cập nhật thành công");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi cập nhật đánh giá: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> deleteReview(@PathVariable("id") Long reviewId) {
        try {
            User loginUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            reviewService.deleteReview(reviewId, loginUser.getUserId());
            return ResponseEntity.ok("Đánh giá đã được xóa thành công");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi xóa đánh giá: " + e.getMessage());
        }
    }
}