package com.esprit.reservation.dto;

import com.esprit.reservation.entity.TableShape;
import com.esprit.reservation.entity.TableStatus;

public record TableResponse(
    Long id,
    Long roomId,
    String tableNumber,
    Integer capacity,
    Integer xPosition,
    Integer yPosition,
    TableShape shape,
    TableStatus status,
    Boolean active
) {}
