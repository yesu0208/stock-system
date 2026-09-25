package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoExitTransactionalService {

    private final OtocoRepository otocoRepository;
    private final OrderService orderService;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;

    @Transactional
    public void triggerExit(OtocoDto dto, OtocoLeg leg) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(dto.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (entity.getOtocoStatus() != OtocoStatus.WAITING_EXIT) {
            return;
        }

        Integer exitPrice = leg == OtocoLeg.TAKE_PROFIT ? entity.getTpTriggerPrice() : entity.getSlTriggerPrice();

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromOtocoExit(dto, exitPrice, leg);

        // 청산용 주식은 진입 체결 시점이 아니라 여기서 일반 매도 주문처럼 예약함.
        // (진입 체결분은 account-server 반영 이후에야 보유 수량에 잡히므로 체결 직후에는 예약할 수 없음)
        // 주문 등록 중 예외: OrderService가 자체 예약분을 환불한 뒤 던지므로 그대로 전파
        // -> 롤백으로 WAITING_EXIT 유지, 트리거 서비스가 북에 다시 등록해 다음 틱에 재시도
        OrderEntity savedOrder = orderService.registerOrder(event, false);

        if (savedOrder == null) {
            // 보유 수량 부족 등으로 예약 실패 (대기 중 사용자가 직접 매도한 경우 등)
            failExit(entity, leg);
            return;
        }

        entity.markCompleted(leg);
        otocoRepository.save(entity);

        // 청산 주문 등록 완료: 이후 부가 작업 실패로 롤백되지 않도록 로그만 남김
        try {
            stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        } catch (Exception e) {
            log.warn("Otoco response delete failed after exit trigger. otocoId={}", entity.getOtocoId(), e);
        }

        otocoResponseEventPublisher.publishExitTriggered(OtocoDto.fromEntity(entity), leg);
    }

    private void failExit(OtocoEntity entity, OtocoLeg leg) {

        log.warn("Otoco exit order rejected (stock reservation failed). otocoId={}, username={}, leg={}",
                entity.getOtocoId(), entity.getUsername(), leg);

        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        try {
            stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        } catch (Exception e) {
            log.warn("Otoco response delete failed after exit failure. otocoId={}", entity.getOtocoId(), e);
        }
        otocoResponseEventPublisher.publishExitFailed(OtocoDto.fromEntity(entity), OtocoResultCode.INTERNAL_ERROR);
    }
}
