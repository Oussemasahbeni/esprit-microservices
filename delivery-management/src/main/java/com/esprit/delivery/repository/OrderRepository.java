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
     * All orders that still contain the given dish and have not yet reached
     * a terminal status. Backs {@code OrderService#handleDishUnavailable}.
     */
    @Query("select distinct o from Order o join o.items i " +
            "where i.dishId = :dishId and o.status not in :terminalStatuses")
    List<Order> findNonTerminalOrdersContainingDish(@Param("dishId") Long dishId,
                                                    @Param("terminalStatuses") List<OrderStatus> terminalStatuses);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId")
    List<Order> findByCustomerIdFetchItems(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.status = :status")
    List<Order> findByStatusFetchItems(@Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdFetchItems(@Param("id") Long id);
}
