package com.esprit.menu.service;

import com.esprit.menu.dto.DishAvailabilityRequest;
import com.esprit.menu.dto.DishRequest;
import com.esprit.menu.dto.DishVariantRequest;
import com.esprit.menu.dto.PromotionRequest;
import com.esprit.menu.entity.Dish;
import com.esprit.menu.entity.MenuCategory;
import com.esprit.menu.entity.Promotion;
import com.esprit.menu.messaging.MenuEventPublisher;
import com.esprit.menu.repository.DishRepository;
import com.esprit.menu.repository.MenuCategoryRepository;
import com.esprit.menu.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuCategoryRepository categoryRepository;
    @Mock
    private DishRepository dishRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private MenuEventPublisher eventPublisher;

    private MenuService menuService;
    private MenuCategory category;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(
                categoryRepository,
                dishRepository,
                promotionRepository,
                eventPublisher,
                new MenuMapper());
        category = MenuCategory.builder().id(1L).name("Mains").displayOrder(1).active(true).build();
    }

    @Test
    void createDishPersistsVariantsIngredientsAndAllergens() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(dishRepository.save(any(Dish.class))).thenAnswer(invocation -> {
            Dish dish = invocation.getArgument(0);
            dish.setId(10L);
            return dish;
        });

        var request = new DishRequest(
                "Truffle Pasta",
                "Fresh pasta",
                BigDecimal.valueOf(28),
                "https://example.test/pasta.jpg",
                true,
                1L,
                Set.of("Pasta", "Mushroom"),
                Set.of("Gluten"),
                List.of(new DishVariantRequest("Large", BigDecimal.valueOf(4), true))
        );

        var response = menuService.createDish(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category().name()).isEqualTo("Mains");
        assertThat(response.ingredients()).contains("Pasta", "Mushroom");
        assertThat(response.variants()).hasSize(1);
        verify(eventPublisher).publishDishCreated(any(Dish.class));
    }

    @Test
    void updateAvailabilityPublishesWhenDishBecomesUnavailable() {
        Dish dish = Dish.builder()
                .id(7L)
                .name("Pistachio Tiramisu")
                .price(BigDecimal.valueOf(13))
                .available(true)
                .category(category)
                .build();
        when(dishRepository.findById(7L)).thenReturn(Optional.of(dish));
        when(dishRepository.save(dish)).thenReturn(dish);

        var response = menuService.updateAvailability(7L, new DishAvailabilityRequest(false));

        assertThat(response.available()).isFalse();
        verify(eventPublisher).publishDishUnavailable(dish);
    }

    @Test
    void createPromotionPublishesPromoCreated() {
        LocalDateTime startsAt = LocalDateTime.now();
        LocalDateTime endsAt = startsAt.plusDays(7);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> {
            Promotion promotion = invocation.getArgument(0);
            promotion.setId(3L);
            return promotion;
        });

        var response = menuService.createPromotion(new PromotionRequest(
                "Lunch Boost",
                "Weekday lunch offer",
                BigDecimal.valueOf(15),
                startsAt,
                endsAt,
                true,
                null,
                1L
        ));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.categoryName()).isEqualTo("Mains");
        verify(eventPublisher).publishPromotionCreated(any(Promotion.class));
    }
}
