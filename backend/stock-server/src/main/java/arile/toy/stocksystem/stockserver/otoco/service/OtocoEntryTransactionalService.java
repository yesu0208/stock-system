package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoEntryTransactionalService {

    private final OtocoRepository otocoRepository;
    private final OrderService orderService;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final AccountApiClient accountApiClient;
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void triggerEntryAndRegisterOrder(OtocoDto dto) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(dto.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (entity.getOtocoStatus() != OtocoStatus.WAITING_ENTRY) {
            return;
        }

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromOtocoEntry(dto);

        // 주문 등록 중 예외: 주문 저장이 이 트랜잭션에 참여하므로 롤백되어야 함.
        // 여기서 환불하면 롤백으로 WAITING_ENTRY가 되살아난 뒤 재발동 시 예약 없이 주문될 수 있으므로
        // 환불하지 않고 예외를 전파 -> 트리거 서비스가 북에 다시 등록해 다음 틱에 재시도 (예약은 그대로 유지)
        OrderEntity savedOrder = orderService.registerOrder(event, true);

        if (savedOrder == null) {
            compensateFailedEntry(entity);
            return;
        }

        entity.setEntryOrderId(savedOrder.getOrderId());
        entity.changeStatus(OtocoStatus.ENTRY_ORDER_PLACED);
        otocoRepository.save(entity);

        // 주문 등록 완료: 이후 부가 작업이 실패해 롤백되면 큐에 들어간 주문과 DB가 어긋나므로 로그만 남김
        try {
            stockServerOtocoResponseRepository.update(entity.getUsername(), entity.getOtocoId(),
                    StockServerOtocoResponseMessage.fromEntity(entity));
        } catch (Exception e) {
            log.warn("Otoco response update failed after entry trigger. otocoId={}", entity.getOtocoId(), e);
        }

        otocoResponseEventPublisher.publishEntryTriggered(OtocoDto.fromEntity(entity));
    }

    private void compensateFailedEntry(OtocoEntity entity) {

        // 롤백 가능한 DB 작업을 먼저 확정한 뒤 되돌릴 수 없는 환불 수행
        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.saveAndFlush(entity);

        long orderAmount = (long) entity.getEntryTriggerPrice() * entity.getOrderQuantity();
        long refundAmount = reserveAmountCalculator.calculateReserveAmount(entity.getLeverageRatio(), orderAmount);

        boolean refunded = accountApiClient.refundReservedCash(entity.getUsername(), refundAmount);

        if (!refunded) {
            log.error("CRITICAL: Otoco entry trigger compensation refund FAILED. " +
                            "Manual intervention required. otocoId={}, username={}",
                    entity.getOtocoId(), entity.getUsername());
        }

        try {
            stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        } catch (Exception e) {
            log.warn("Otoco response delete failed after entry compensation. otocoId={}", entity.getOtocoId(), e);
        }
        otocoResponseEventPublisher.publishEntryFailed(OtocoDto.fromEntity(entity), OtocoResultCode.ENTRY_FAILED);
    }
}
