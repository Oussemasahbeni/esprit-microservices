package com.esprit.menu.service;

import com.esprit.menu.dto.CategoryRequest;
import com.esprit.menu.dto.CategoryResponse;
import com.esprit.menu.dto.DishAvailabilityRequest;
import com.esprit.menu.dto.DishRequest;
import com.esprit.menu.dto.DishResponse;
import com.esprit.menu.dto.MenuResponse;
import com.esprit.menu.dto.MenuSummaryResponse;
import com.esprit.menu.dto.PromotionRequest;
import com.esprit.menu.dto.PromotionResponse;
import com.esprit.menu.entity.Dish;
import com.esprit.menu.entity.DishVariant;
import com.esprit.menu.entity.MenuCategory;
import com.esprit.menu.entity.Promotion;
import com.esprit.menu.messaging.MenuEventPublisher;
import com.esprit.menu.repository.DishRepository;
import com.esprit.menu.repository.MenuCategoryRepository;
import com.esprit.menu.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final PromotionRepository promotionRepository;
    private final MenuEventPublisher eventPublisher;
    private final MenuMapper mapper;

    @Transactional(readOnly = true)
    public MenuResponse getMenu() {
        List<CategoryResponse> categories = listCategories();
        List<DishResponse> dishes = listDishes();
        List<PromotionResponse> promotions = listPromotions();
        long activePromotions = promotions.stream().filter(PromotionResponse::active).count();
        return new MenuResponse(
                new MenuSummaryResponse(
                        dishes.size(),
                        dishRepository.countByAvailableTrue(),
                        dishRepository.countByAvailableFalse(),
                        categories.size(),
                        activePromotions),
                categories,
                dishes,
                promotions
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(mapper::toCategoryResponse)
                .toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists: " + request.name());
        }
        MenuCategory category = MenuCategory.builder()
                .name(request.name().trim())
                .description(clean(request.description()))
                .displayOrder(request.displayOrder())
                .active(request.active())
                .build();
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<DishResponse> listDishes() {
        return dishRepository.findAllByOrderByCategoryDisplayOrderAscNameAsc().stream()
                .map(mapper::toDishResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DishResponse getDish(Long id) {
        return mapper.toDishResponse(getDishEntity(id));
    }

    public DishResponse createDish(DishRequest request) {
        MenuCategory category = getCategoryEntity(request.categoryId());
        Dish dish = Dish.builder()
                .name(request.name().trim())
                .description(clean(request.description()))
                .price(request.price())
                .photoUrl(clean(request.photoUrl()))
                .available(request.available())
                .category(category)
                .ingredients(cleanSet(request.ingredients()))
                .allergens(cleanSet(request.allergens()))
                .build();
        dish.replaceVariants(toVariants(request));
        return mapper.toDishResponse(dishRepository.save(dish));
    }

    public DishResponse updateDish(Long id, DishRequest request) {
        Dish dish = getDishEntity(id);
        boolean wasAvailable = dish.isAvailable();
        dish.setName(request.name().trim());
        dish.setDescription(clean(request.description()));
        dish.setPrice(request.price());
        dish.setPhotoUrl(clean(request.photoUrl()));
        dish.setAvailable(request.available());
        dish.setCategory(getCategoryEntity(request.categoryId()));
        dish.setIngredients(cleanSet(request.ingredients()));
        dish.setAllergens(cleanSet(request.allergens()));
        dish.replaceVariants(toVariants(request));
        Dish saved = dishRepository.save(dish);
        if (wasAvailable && !saved.isAvailable()) {
            eventPublisher.publishDishUnavailable(saved);
        }
        return mapper.toDishResponse(saved);
    }

    public DishResponse updateAvailability(Long id, DishAvailabilityRequest request) {
        Dish dish = getDishEntity(id);
        boolean wasAvailable = dish.isAvailable();
        dish.setAvailable(request.available());
        Dish saved = dishRepository.save(dish);
        if (wasAvailable && !saved.isAvailable()) {
            eventPublisher.publishDishUnavailable(saved);
        }
        return mapper.toDishResponse(saved);
    }

    public void deleteDish(Long id) {
        Dish dish = getDishEntity(id);
        dishRepository.delete(dish);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> listPromotions() {
        return promotionRepository.findAllByOrderByStartsAtDesc().stream()
                .map(mapper::toPromotionResponse)
                .toList();
    }

    public PromotionResponse createPromotion(PromotionRequest request) {
        Promotion promotion = Promotion.builder()
                .name(request.name().trim())
                .description(clean(request.description()))
                .discountPercent(request.discountPercent())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .active(request.active())
                .dish(request.dishId() != null ? getDishEntity(request.dishId()) : null)
                .category(request.categoryId() != null ? getCategoryEntity(request.categoryId()) : null)
                .build();
        Promotion saved = promotionRepository.save(promotion);
        eventPublisher.publishPromotionCreated(saved);
        return mapper.toPromotionResponse(saved);
    }

    private MenuCategory getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + id));
    }

    private Dish getDishEntity(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dish not found: " + id));
    }

    private List<DishVariant> toVariants(DishRequest request) {
        if (request.variants() == null) {
            return List.of();
        }
        return request.variants().stream()
                .map(variant -> DishVariant.builder()
                        .name(variant.name().trim())
                        .priceDelta(variant.priceDelta())
                        .available(variant.available())
                        .build())
                .toList();
    }

    private Set<String> cleanSet(Set<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
                .map(this::clean)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
