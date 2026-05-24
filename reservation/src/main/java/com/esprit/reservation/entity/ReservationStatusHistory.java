package com.esprit.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "old_status", length = 30)
    @Enumerated(EnumType.STRING)
    private ReservationStatus oldStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReservationStatus newStatus;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(length = 255)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
