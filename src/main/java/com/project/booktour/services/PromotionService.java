package com.project.booktour.services;

import com.project.booktour.dtos.PromotionDTO;
import com.project.booktour.models.Promotion;
import com.project.booktour.models.PromotionStatus;
import com.project.booktour.repositories.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    // Tạo mã giảm giá ngẫu nhiên
    public Promotion createPromotion(PromotionDTO promotionDTO) {
        // Tạo mã code ngẫu nhiên
        String generatedCode = generateRandomCode(8);

        // Kiểm tra xem mã có duy nhất hay không
        while (promotionRepository.findByCode(generatedCode).isPresent()) {
            generatedCode = generateRandomCode(8);
        }

        Promotion promotion = Promotion.builder()
                .description(promotionDTO.getDescription())
                .discount(promotionDTO.getDiscount())
                .startDate(promotionDTO.getStartDate())
                .endDate(promotionDTO.getEndDate())
                .quantity(promotionDTO.getQuantity())
                .code(generatedCode)
                .status(PromotionStatus.ACTIVE)
                .build();

        // Kiểm tra ngày hợp lệ
        if (promotion.getStartDate().isAfter(promotion.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        if (promotion.getEndDate().isBefore(LocalDate.now())) {
            promotion.setStatus(PromotionStatus.EXPIRED);
        }

        return promotionRepository.save(promotion);
    }

    // Tạo mã ngẫu nhiên
    private String generateRandomCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        return code.toString();
    }

    // Kiểm tra mã giảm giá có hợp lệ không
    public Promotion validatePromotion(String code) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại: " + code));

        if (promotion.getStatus() == PromotionStatus.EXPIRED ||
                promotion.getEndDate().isBefore(LocalDate.now())) {
            promotion.setStatus(PromotionStatus.EXPIRED);
            promotionRepository.save(promotion);
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn");
        }

        if (promotion.getQuantity() <= 0) {
            promotion.setStatus(PromotionStatus.INACTIVE);
            promotionRepository.save(promotion);
            throw new IllegalArgumentException("Mã giảm giá đã hết số lượng");
        }

        return promotion;
    }

    // Giảm số lượng mã giảm giá sau khi sử dụng
    public void decreasePromotionQuantity(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá với id: " + promotionId));

        if (promotion.getQuantity() > 0) {
            promotion.setQuantity(promotion.getQuantity() - 1);
            if (promotion.getQuantity() == 0) {
                promotion.setStatus(PromotionStatus.INACTIVE);
            }
            promotionRepository.save(promotion);
        }
    }
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findAll().stream()
                .filter(p -> p.getStatus() == PromotionStatus.ACTIVE
                        && !p.getEndDate().isBefore(LocalDate.now())
                        && p.getQuantity() > 0)
                .collect(Collectors.toList());
    }
    public void increasePromotionQuantity(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá với id: " + promotionId));
        promotion.setQuantity(promotion.getQuantity() + 1);
        if (promotion.getQuantity() > 0 && promotion.getStatus() == PromotionStatus.INACTIVE) {
            promotion.setStatus(PromotionStatus.ACTIVE);
        }
        promotionRepository.save(promotion);
    }
    public void deletePromotion(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá với id: " + promotionId));
        promotionRepository.delete(promotion);
    }
}