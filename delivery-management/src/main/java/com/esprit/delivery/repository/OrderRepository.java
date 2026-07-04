package com.esprit.delivery.repository;

import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByAssignment_Driver_Id(Long driverId);

    /**
     * All orders that still contain the given dish and have not yet reached
     * a terminal status. Backs {@code OrderService#handleDishUnavailable}.
     */
    @Query("select distinct o from Order o join o.items i " +
            "where i.dishId = :dishId and o.status not in :terminalStatuses")
    List<Order> findNonTerminalOrdersContainingDish(@Param("dishId") Long dishId,
                                                    @Param("terminalStatuses") List<OrderStatus> terminalStatuses);
}
