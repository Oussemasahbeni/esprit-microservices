package com.esprit.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import com.esprit.reservation.domain.GuestsCount;

@Entity
@Table(name = "waitlist_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", nullable = false, length = 100)
    private String keycloakUserId;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private com.esprit.reservation.domain.EmailAddress customerEmail;

    @Column(name = "customer_phone", length = 30)
    private com.esprit.reservation.domain.PhoneNumber customerPhone;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Column(name = "requested_time", nullable = false)
    private LocalTime requestedTime;

    @Column(name = "guests_count", nullable = false)
    private GuestsCount guestsCount;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Builder.Default
    private Integer priority = 0;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (priority == null) {
            priority = 0;
        }
        if (status == null) {
            status = WaitlistStatus.WAITING;
        }
    }
}
