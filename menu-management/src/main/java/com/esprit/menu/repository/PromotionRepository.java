package com.esprit.menu.repository;

import com.esprit.menu.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findAllByOrderByStartsAtDesc();
}
