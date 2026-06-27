package com.esprit.menu.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "dish")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @ElementCollection
    @CollectionTable(name = "dish_ingredient", joinColumns = @JoinColumn(name = "dish_id"))
    @Column(name = "ingredient", nullable = false, length = 120)
    @Builder.Default
    private Set<String> ingredients = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "dish_allergen", joinColumns = @JoinColumn(name = "dish_id"))
    @Column(name = "allergen", nullable = false, length = 120)
    @Builder.Default
    private Set<String> allergens = new LinkedHashSet<>();

    @OneToMany(mappedBy = "dish", cascade = ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    @Builder.Default
    private List<DishVariant> variants = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void replaceVariants(List<DishVariant> newVariants) {
        variants.clear();
        newVariants.forEach(variant -> {
            variant.setDish(this);
            variants.add(variant);
        });
    }
}
