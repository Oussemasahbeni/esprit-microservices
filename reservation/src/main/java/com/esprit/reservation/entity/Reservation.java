package com.esprit.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import com.esprit.reservation.domain.ReservationCode;
import com.esprit.reservation.domain.GuestsCount;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 40)
    private ReservationCode reservationCode;

    @Column(name = "keycloak_user_id", nullable = false, length = 100)
    private String keycloakUserId;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 150)
    private com.esprit.reservation.domain.EmailAddress customerEmail;

    @Column(name = "customer_phone", length = 30)
    private com.esprit.reservation.domain.PhoneNumber customerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "guests_count", nullable = false)
    private GuestsCount guestsCount;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(length = 30)
    @Builder.Default
    private String source = "ONLINE";

    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ReservationStatusHistory> statusHistory = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<ReservationPreOrderItem> preOrderItems = new java.util.ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void cancel(String reason, String changedBy) {
        if (this.status == ReservationStatus.CANCELLED) {
            return;
        }
        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.CANCELLED;
        this.cancellationReason = reason;
        this.statusHistory.add(
            ReservationStatusHistory.builder()
                .reservation(this)
                .oldStatus(oldStatus)
                .newStatus(ReservationStatus.CANCELLED)
                .changedBy(changedBy)
                .reason(reason)
                .build()
        );
    }

    public void seat(String changedBy) {
        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.SEATED;
        this.statusHistory.add(
            ReservationStatusHistory.builder()
                .reservation(this)
                .oldStatus(oldStatus)
                .newStatus(ReservationStatus.SEATED)
                .changedBy(changedBy)
                .reason("Customer arrived and is seated.")
                .build()
        );
    }

    public void complete(String changedBy) {
        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.COMPLETED;
        this.statusHistory.add(
            ReservationStatusHistory.builder()
                .reservation(this)
                .oldStatus(oldStatus)
                .newStatus(ReservationStatus.COMPLETED)
                .changedBy(changedBy)
                .reason("Dining completed.")
                .build()
        );
    }

    public void markNoShow(String changedBy) {
        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.NO_SHOW;
        this.statusHistory.add(
            ReservationStatusHistory.builder()
                .reservation(this)
                .oldStatus(oldStatus)
                .newStatus(ReservationStatus.NO_SHOW)
                .changedBy(changedBy)
                .reason("Customer did not show up.")
                .build()
        );
    }

    public void assignTable(RestaurantTable newTable, String changedBy) {
        if (newTable.getCapacity() < this.guestsCount.value()) {
            throw new IllegalArgumentException("Table capacity is smaller than reservation guests count.");
        }
        RestaurantTable oldTable = this.table;
        this.table = newTable;
        this.statusHistory.add(
            ReservationStatusHistory.builder()
                .reservation(this)
                .oldStatus(this.status)
                .newStatus(this.status)
                .changedBy(changedBy)
                .reason("Table reassigned from " + (oldTable != null ? oldTable.getTableNumber() : "none") + " to " + newTable.getTableNumber())
                .build()
        );
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (source == null) {
            source = "ONLINE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
