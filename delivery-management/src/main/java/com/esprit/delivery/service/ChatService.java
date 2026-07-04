package com.esprit.delivery.service;

import com.esprit.delivery.dto.DeliveryDtos.SendChatMessageRequest;
import com.esprit.delivery.entity.ChatMessage;

import java.util.List;

/**
 * Use cases for the real-time chat channel between a customer and the driver
 * assigned to their order. The real-time push itself is handled over
 * WebSocket/STOMP (Spring's {@code SimpMessagingTemplate}, configured
 * separately); this service is the persistence/business-rule layer behind it.
 */
public interface ChatService {

    /**
     * Persists a message and forwards it to the recipient's WebSocket session.
     * Rejects messages for orders that are not in an active delivery state
     * (e.g. already DELIVERED or CANCELLED).
     */
    ChatMessage sendMessage(SendChatMessageRequest request);

    /**
     * Retrieves the full chat history for an order, in chronological order.
     */
    List<ChatMessage> getConversation(Long orderId);
}
