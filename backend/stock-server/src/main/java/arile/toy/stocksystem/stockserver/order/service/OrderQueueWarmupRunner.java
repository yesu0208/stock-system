package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.external.stock.manager.ExternalStockProperties;
import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueueWarmupRunner {

    private final OrderRepository orderRepository;
    private final OrderQueueRegistry orderQueueRegistry;
    private final ExternalStockProperties externalStockProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        List<String> myStockCodes = externalStockProperties.getOpen();
        if (myStockCodes.isEmpty()) {
            return;
        }

        List<OrderEntity> unfilledOrders = orderRepository.findAllUnfilled(myStockCodes);

        for (OrderEntity order : unfilledOrders) {
            orderQueueRegistry.orderEnqueue(OrderDto.fromEntity(order));
        }

        log.info("Order queue warm-up complete. stockCodes={}, restoredOrders={}",
                myStockCodes, unfilledOrders.size());
    }
}
