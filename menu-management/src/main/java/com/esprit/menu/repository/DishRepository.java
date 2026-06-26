package com.esprit.menu.repository;

import com.esprit.menu.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findAllByOrderByCategoryDisplayOrderAscNameAsc();

    long countByAvailableTrue();

    long countByAvailableFalse();
}
