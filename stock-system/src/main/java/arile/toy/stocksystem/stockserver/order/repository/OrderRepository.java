package arile.toy.stocksystem.stockserver.order.repository;

import arile.toy.stocksystem.stockserver.order.dto.OrderStatus;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.orderId = :orderId")
    Optional<OrderEntity> findByIdForUpdate(@Param("orderId") Long orderId);

    default List<OrderEntity> findAllUnfilled() {
        List<OrderStatus> openStatuses = Arrays.stream(OrderStatus.values())
                .filter(OrderStatus::isOpen)
                .toList();
        return findAllByOrderStatusIn(openStatuses);
    }

    List<OrderEntity> findAllByOrderStatusIn(List<OrderStatus> statuses);
}
