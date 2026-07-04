package com.esprit.reservation.service;

import com.esprit.reservation.entity.WaitlistEntry;
import com.esprit.reservation.entity.WaitlistStatus;
import com.esprit.reservation.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final ReservationService reservationService;

    public List<WaitlistEntry> getActiveWaitlist() {
        return waitlistRepository.findByStatus(WaitlistStatus.WAITING);
    }

    @Transactional
    public void promoteWaitlistEntry(Long id) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found: " + id));

        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new IllegalArgumentException("Waitlist entry is not in WAITING status.");
        }

        boolean promoted = reservationService.promoteWaitlistEntryNow(entry);
        if (!promoted) {
            throw new IllegalStateException(
                    "No table is available for this party size and time yet — still waiting.");
        }
    }

    @Transactional
    public WaitlistEntry cancelWaitlistEntry(Long id) {
        WaitlistEntry entry = waitlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found: " + id));
        entry.setStatus(WaitlistStatus.CANCELLED);
        return waitlistRepository.save(entry);
    }
}
