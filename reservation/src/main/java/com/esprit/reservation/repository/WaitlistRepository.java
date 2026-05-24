package com.esprit.reservation.repository;

import com.esprit.reservation.entity.WaitlistEntry;
import com.esprit.reservation.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    
    List<WaitlistEntry> findByStatus(WaitlistStatus status);

    List<WaitlistEntry> findByRequestedDateAndStatusOrderByPriorityDescCreatedAtAsc(LocalDate requestedDate, WaitlistStatus status);
}
