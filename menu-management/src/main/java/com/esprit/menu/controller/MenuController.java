package com.esprit.menu.controller;

import com.esprit.menu.dto.CategoryRequest;
import com.esprit.menu.dto.CategoryResponse;
import com.esprit.menu.dto.DishAvailabilityRequest;
import com.esprit.menu.dto.DishRequest;
import com.esprit.menu.dto.DishResponse;
import com.esprit.menu.dto.MenuResponse;
import com.esprit.menu.dto.PromotionRequest;
import com.esprit.menu.dto.PromotionResponse;
import com.esprit.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public MenuResponse getMenu() {
        return menuService.getMenu();
    }

    @GetMapping("/dishes")
    public List<DishResponse> listDishes() {
        return menuService.listDishes();
    }

    @GetMapping("/dishes/{id}")
    public DishResponse getDish(@PathVariable Long id) {
        return menuService.getDish(id);
    }

    @PostMapping("/dishes")
    public ResponseEntity<DishResponse> createDish(@Valid @RequestBody DishRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createDish(request));
    }

    @PutMapping("/dishes/{id}")
    public DishResponse updateDish(@PathVariable Long id, @Valid @RequestBody DishRequest request) {
        return menuService.updateDish(id, request);
    }

    @PatchMapping("/dishes/{id}/availability")
    public DishResponse updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody DishAvailabilityRequest request) {
        return menuService.updateAvailability(id, request);
    }

    @DeleteMapping("/dishes/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable Long id) {
        menuService.deleteDish(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return menuService.listCategories();
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createCategory(request));
    }

    @GetMapping("/promotions")
    public List<PromotionResponse> listPromotions() {
        return menuService.listPromotions();
    }

    @PostMapping("/promotions")
    public ResponseEntity<PromotionResponse> createPromotion(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createPromotion(request));
    }
}
