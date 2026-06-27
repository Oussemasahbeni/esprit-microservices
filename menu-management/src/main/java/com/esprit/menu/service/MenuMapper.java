package com.esprit.menu.service;

import com.esprit.menu.dto.CategoryResponse;
import com.esprit.menu.dto.DishResponse;
import com.esprit.menu.dto.DishVariantResponse;
import com.esprit.menu.dto.PromotionResponse;
import com.esprit.menu.entity.Dish;
import com.esprit.menu.entity.DishVariant;
import com.esprit.menu.entity.MenuCategory;
import com.esprit.menu.entity.Promotion;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class MenuMapper {

    public CategoryResponse toCategoryResponse(MenuCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public DishVariantResponse toVariantResponse(DishVariant variant) {
        return new DishVariantResponse(
                variant.getId(),
                variant.getName(),
                variant.getPriceDelta(),
                variant.isAvailable()
        );
    }

    public DishResponse toDishResponse(Dish dish) {
        return new DishResponse(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getPrice(),
                dish.getPhotoUrl(),
                dish.isAvailable(),
                toCategoryResponse(dish.getCategory()),
                dish.getIngredients(),
                dish.getAllergens(),
                dish.getVariants().stream()
                        .sorted(Comparator.comparing(DishVariant::getName))
                        .map(this::toVariantResponse)
                        .toList(),
                dish.getCreatedAt(),
                dish.getUpdatedAt()
        );
    }

    public PromotionResponse toPromotionResponse(Promotion promotion) {
        Dish dish = promotion.getDish();
        MenuCategory category = promotion.getCategory();
        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getDiscountPercent(),
                promotion.getStartsAt(),
                promotion.getEndsAt(),
                promotion.isActive(),
                dish != null ? dish.getId() : null,
                dish != null ? dish.getName() : null,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                promotion.getCreatedAt(),
                promotion.getUpdatedAt()
        );
    }
}
