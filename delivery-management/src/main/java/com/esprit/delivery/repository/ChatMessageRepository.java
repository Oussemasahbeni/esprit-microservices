package com.esprit.delivery.repository;

import com.esprit.delivery.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByOrder_IdOrderBySentAtAsc(Long orderId);
}
