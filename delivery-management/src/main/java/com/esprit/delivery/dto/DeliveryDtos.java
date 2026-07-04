package com.esprit.delivery.dto;

import com.esprit.delivery.enums.OrderStatus;
import com.esprit.delivery.enums.SenderType;
import lombok.*;

public class DeliveryDtos {

    /**
     * Request payload for {@code PATCH /orders/{id}/status}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateOrderStatusRequest {
        private OrderStatus newStatus;
        /**
         * Id of the driver performing the update (for authorization/audit).
         */
        private Long driverId;
    }

    /**
     * Request payload for {@code POST /assign-driver}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignDriverRequest {
        private Long orderId;
        /**
         * Optional: if null, the service auto-selects the nearest available driver.
         */
        private Long driverId;
    }

    /**
     * Request payload for sending a chat message via WebSocket/REST fallback.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendChatMessageRequest {
        private Long orderId;
        private SenderType senderType;
        private Long senderId;
        private String content;
    }

    /**
     * Request payload for {@code POST /orders/{id}/rate-driver}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RateDriverRequest {
        private Long orderId;
        private Long customerId;
        private Integer score;
        private String comment;
    }

    /**
     * Request payload for drivers pushing their GPS position periodically.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateDriverLocationRequest {
        private Long driverId;
        private Double latitude;
        private Double longitude;
    }
}
