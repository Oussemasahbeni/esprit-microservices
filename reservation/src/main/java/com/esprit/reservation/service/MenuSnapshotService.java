package com.esprit.reservation.service;

import com.esprit.reservation.client.MenuManagementClient;
import com.esprit.reservation.client.MenuSnapshot;
import com.esprit.reservation.dto.MenuSnapshotResponse;
import com.esprit.reservation.dto.MenuSnapshotResponse.DishOption;
import com.esprit.reservation.dto.MenuSnapshotResponse.PromotionOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the booking flow's "available menu" view with a live call to menu-management —
 * no menu/promotion data is duplicated or cached in this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuSnapshotService {

    private final MenuManagementClient menuManagementClient;

    public MenuSnapshotResponse getSnapshot() {
        MenuSnapshot menu = fetchMenu();
        LocalDateTime now = LocalDateTime.now();

        List<DishOption> dishes = menu.dishes().stream()
                .filter(MenuSnapshot.Dish::available)
                .map(this::toDishOption)
                .toList();

        List<PromotionOption> promotions = menu.promotions().stream()
                .filter(MenuSnapshot.Promotion::active)
                .filter(p -> !now.isBefore(p.startsAt()) && !now.isAfter(p.endsAt()))
                .map(this::toPromotionOption)
                .toList();

        return new MenuSnapshotResponse(dishes, promotions);
    }

    private MenuSnapshot fetchMenu() {
        try {
            return menuManagementClient.getMenu();
        } catch (Exception e) {
            log.warn("menu-management unavailable, returning empty menu snapshot: {}", e.getMessage());
            return new MenuSnapshot(List.of(), List.of());
        }
    }

    private DishOption toDishOption(MenuSnapshot.Dish dish) {
        String categoryName = dish.category() != null ? dish.category().name() : null;
        return new DishOption(dish.id(), dish.name(), dish.price(), categoryName);
    }

    private PromotionOption toPromotionOption(MenuSnapshot.Promotion promo) {
        return new PromotionOption(promo.id(), promo.name(), promo.discountPercent(),
                promo.dishId(), promo.categoryId(), promo.startsAt(), promo.endsAt());
    }
}
