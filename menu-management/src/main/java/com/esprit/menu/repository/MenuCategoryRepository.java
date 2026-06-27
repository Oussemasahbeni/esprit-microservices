package com.esprit.menu.repository;

import com.esprit.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<MenuCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
