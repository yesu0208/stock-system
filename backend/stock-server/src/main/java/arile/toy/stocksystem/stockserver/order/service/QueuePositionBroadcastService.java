package arile.toy.stocksystem.stockserver.order.service;

import arile.toy.stocksystem.stockserver.order.dto.OrderDto;
import arile.toy.stocksystem.stockserver.order.dto.OrderQueueRegistry;
import arile.toy.stocksystem.stockserver.order.dto.OrderType;
import arile.toy.stocksystem.stockserver.order.event.QueuePositionEvent;
import arile.toy.stocksystem.stockserver.order.event.publisher.QueuePositionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 특정 종목/방향(매수/매도)의 미체결 주문 큐가 변경된 직후 호출
 * 큐 전체를 가격-시간 우선순위로 스냅샷한 뒤, 각 주문마다
 * "자신보다 앞선 주문들의 미체결 잔량 합계"를 계산해서
 * 해당 주문의 소유자에게 개별로 발행
 *
 * 큐(OrderQueueRegistry)는 정렬 책임만 지고, "앞선 수량이 몇 주인지"
 * 계산하는 도메인 로직은 이 서비스가 전담.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueuePositionBroadcastService {

    private final OrderQueueRegistry orderQueueRegistry;
    private final QueuePositionEventPublisher queuePositionEventPublisher;

    public void broadcast(String stockCode, OrderType orderType) {
        List<OrderDto> ranked = orderQueueRegistry.snapshotRanked(stockCode, orderType);

        long quantityAhead = 0;
        for (OrderDto order : ranked) {
            try {
                queuePositionEventPublisher.publish(
                        QueuePositionEvent.of(order.orderId(), order.username(), stockCode, quantityAhead)
                );
            } catch (Exception e) {
                log.warn("QueuePositionEvent 발행 실패. orderId={}, username={}",
                        order.orderId(), order.username(), e);
            }
            quantityAhead += order.remainingQuantity();
        }
    }
}
