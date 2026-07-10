package com.esprit.delivery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Local, minimal cache of customer-facing delivery data.
 *
 * <p>Identity (name, email, phone...) stays owned by the employee/IAM microservice and is validated
 * via {@code EmployeeServiceClient} — it is never duplicated here. This entity only stores what the
 * delivery microservice itself needs: a default address to prefill new orders with, and running
 * loyalty/order stats.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Foreign reference to the user record in the employee/IAM microservice. */
  @Column(nullable = false, unique = true)
  private String customerId;

  @Embedded private DeliveryAddress defaultDeliveryAddress;

  @Builder.Default private Integer loyaltyPoints = 0;

  @Builder.Default private Integer totalOrders = 0;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp private LocalDateTime updatedAt;
}
