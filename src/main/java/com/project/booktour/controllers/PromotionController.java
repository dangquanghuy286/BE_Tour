package com.project.booktour.controllers;

import com.project.booktour.dtos.PromotionDTO;
import com.project.booktour.models.Promotion;
import com.project.booktour.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // Endpoint tạo mã giảm giá
    @PostMapping
    public ResponseEntity<Promotion> createPromotion(@RequestBody PromotionDTO promotionDTO) {
        Promotion promotion = promotionService.createPromotion(promotionDTO);
        return ResponseEntity.ok(promotion);
    }

    // Endpoint lấy danh sách mã giảm giá đang hoạt động
    @GetMapping
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();
        return ResponseEntity.ok(promotions);
    }

    // Endpoint kiểm tra và áp dụng mã giảm giá
    @GetMapping("/validate/{code}")
    public ResponseEntity<Promotion> validatePromotion(@PathVariable String code) {
        Promotion promotion = promotionService.validatePromotion(code);
        return ResponseEntity.ok(promotion);
    }

    // Endpoint xóa mã giảm giá
    @DeleteMapping("/{promotionId}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long promotionId) {
        promotionService.deletePromotion(promotionId);
        return ResponseEntity.noContent().build();
    }
}