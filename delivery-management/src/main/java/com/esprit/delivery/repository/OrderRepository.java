package com.esprit.delivery.repository;

import com.esprit.delivery.entity.Order;
import com.esprit.delivery.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByAssignment_Driver_Id(Long driverId);

    /**
     * Eagerly fetches {@code items} so it's safe to serialize the result
     * after the transaction closes (open-in-view is disabled for this
     * service, so lazy collections can't be touched from the HTTP layer).
     */
    @Query("select o from Order o left join fetch o.items where o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("select distinct o from Order o left join fetch o.items where o.customerId = :customerId")
    List<Order> findByCustomerIdWithItems(@Param("customerId") Long customerId);

    @Query("select distinct o from Order o left join fetch o.items where o.status = :status")
    List<Order> findByStatusWithItems(@Param("status") OrderStatus status);

    /**
     * All orders that still contain the given dish and have not yet reached
     * a terminal status. Backs {@code OrderService#handleDishUnavailable}.
     */
    @Query("select distinct o from Order o join o.items i " +
            "where i.dishId = :dishId and o.status not in :terminalStatuses")
    List<Order> findNonTerminalOrdersContainingDish(@Param("dishId") Long dishId,
                                                    @Param("terminalStatuses") List<OrderStatus> terminalStatuses);
}
