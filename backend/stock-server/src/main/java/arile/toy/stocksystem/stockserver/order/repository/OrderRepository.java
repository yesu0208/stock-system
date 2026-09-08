package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.orderId = :orderId")
    Optional<OrderEntity> findByIdForUpdate(@Param("orderId") Long orderId);

    default List<OrderEntity> findAllUnfilled(List<String> stockCodes) {
        List<OrderStatus> openStatuses = Arrays.stream(OrderStatus.values())
                .filter(OrderStatus::isOpen)
                .toList();
        return findAllByOrderStatusInAndStockCodeIn(openStatuses, stockCodes);
    }

    List<OrderEntity> findAllByOrderStatusInAndStockCodeIn(List<OrderStatus> statuses, List<String> stockCodes);

    @Query("""
            select o from OrderEntity o
            where o.username = :username
            and (:stockCode is null or o.stockCode = :stockCode)
            and (:statuses is null or o.orderStatus in :statuses)
            and (:from is null or o.orderTime >= :from)
            and (:to is null or o.orderTime <= :to)
            order by o.orderTime desc
            """)
    Page<OrderEntity> search(
            @Param("username") String username,
            @Param("stockCode") String stockCode,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
