package arile.toy.stocksystem.stockserver.otococancel.service;

import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.order.service.ReserveAmountCalculator;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoEntryBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.otococancel.dto.OtocoCancelErrorCode;
import arile.toy.stocksystem.stockserver.otococancel.entity.OtocoCancelEntity;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelRequestEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.OtocoCancelResponseEvent;
import arile.toy.stocksystem.stockserver.otococancel.event.publisher.OtocoCancelResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otococancel.repository.OtocoCancelRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoCancelService {

    private final OtocoRepository otocoRepository;
    private final OtocoCancelRepository otocoCancelRepository;
    private final OtocoEntryBookRegistry otocoEntryBookRegistry;
    private final OtocoExitBookRegistry otocoExitBookRegistry;
    private final OtocoCancelResponseEventPublisher otocoCancelResponseEventPublisher;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final AccountApiClient accountApiClient;
    private final CancelService cancelService;
    private final ReserveAmountCalculator reserveAmountCalculator;

    @Transactional
    public void registerCancel(OtocoCancelRequestEvent request) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(request.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        // 요청자가 OTOCO 소유자가 아니거나 종목코드가 다르면 취소하지 않음 (타인 OTOCO 취소 방지)
        // 취소 응답은 소유자 채널로 발행되므로, 거부 시에는 응답을 보내지 않고 로그만 남긴다
        if (request.username() == null
                || !request.username().equals(entity.getUsername())
                || !entity.getStockCode().equals(request.stockCode())) {
            log.warn("Otoco cancel rejected: not the owner or stock code mismatch. otocoId={}, requester={}, stockCode={}",
                    request.otocoId(), request.username(), request.stockCode());
            return;
        }

        switch (entity.getOtocoStatus()) {

            case CANCELED -> otocoCancelResponseEventPublisher.publish(
                    OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.ALREADY_CANCELLED));

            case COMPLETED -> otocoCancelResponseEventPublisher.publish(
                    OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.ALREADY_COMPLETED));

            default -> {
                OtocoStatus previousStatus = entity.getOtocoStatus();
                try {
                    cancelOpen(entity);
                    publishSuccess(entity);
                } catch (Exception e) {
                    log.error("Otoco cancel({}) failed. otocoId={}", previousStatus, entity.getOtocoId(), e);
                    otocoCancelResponseEventPublisher.publish(
                            OtocoCancelResponseEvent.of(entity, false, OtocoCancelErrorCode.INTERNAL_ERROR));
                    // 예외를 삼키면 일부 작업만 커밋되고 예약분이 남을 수 있음
                    // → 다시 던져 트랜잭션을 롤백하고 컨슈머 재시도로 처리
                    throw e;
                }
            }
        }
    }

    @Transactional
    public void forceCancelOtoco(Long otocoId) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(otocoId)
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (!entity.getOtocoStatus().isOpen()) {
            return; // 이미 종료 상태 - 아무 것도 하지 않음
        }

        try {
            cancelOpen(entity);
            publishSuccess(entity);
        } catch (Exception e) {
            // 장 마감 정리용: 남은 예약은 정산에서 해제됨
            log.error("Force otoco cancel failed. otocoId={}", otocoId, e);
        }
    }

    /**
     * 진행 중인 OTOCO를 단계별로 취소.
     * 롤백 가능한 DB 작업(상태·취소 이력) → 되돌릴 수 없는 환불 → 메모리 북 제거 순으로 처리.
     */
    private void cancelOpen(OtocoEntity entity) {

        OtocoStatus previousStatus = entity.getOtocoStatus();

        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);
        otocoCancelRepository.saveAndFlush(OtocoCancelEntity.of(entity.getOtocoId()));

        switch (previousStatus) {

            case WAITING_ENTRY -> {
                refundEntryReservation(entity);
                removeFromBook(() -> otocoEntryBookRegistry.remove(entity.getStockCode(), entity.getOtocoId()), entity);
            }

            // 진입 주문이 큐에 있음: 주문 취소가 주문의 남은 예약분을 환불함
            // (주문 취소 훅은 OTOCO가 이미 CANCELED라 추가 처리하지 않음)
            case ENTRY_ORDER_PLACED -> cancelService.forceCancel(entity.getEntryOrderId());

            // 청산용 주식은 청산 발동 시점에 예약하므로 WAITING_EXIT 단계에는 환불할 예약이 없음
            case WAITING_EXIT ->
                    removeFromBook(() -> otocoExitBookRegistry.remove(entity.getStockCode(), entity.getOtocoId()), entity);

            default -> throw new IllegalStateException("Not an open otoco status: " + previousStatus);
        }
    }

    private void refundEntryReservation(OtocoEntity entity) {

        LeverageRatio leverageRatio = entity.getLeverageRatio();
        long orderAmount = (long) entity.getEntryTriggerPrice() * entity.getOrderQuantity();
        long refundAmount = reserveAmountCalculator.calculateReserveAmount(leverageRatio, orderAmount);

        boolean refunded = accountApiClient.refundReservedCash(entity.getUsername(), refundAmount);
        if (!refunded) {
            log.error("Otoco cash refund failed. otocoId={}, username={}", entity.getOtocoId(), entity.getUsername());
            throw new IllegalStateException("Cash refund failed");
        }
    }

    /** 환불 이후 단계: 실패해도 예외를 던지지 않음 (롤백·재시도 시 이중 환불 방지). 북에 남아도 발동 시 상태 검사로 무시됨 */
    private void removeFromBook(Runnable removal, OtocoEntity entity) {
        try {
            removal.run();
        } catch (Exception e) {
            log.warn("Otoco book remove failed after cancel. otocoId={}", entity.getOtocoId(), e);
        }
    }

    private void publishSuccess(OtocoEntity entity) {
        // 취소는 이미 완료됨: 응답 캐시 삭제 실패가 롤백·재시도로 이어지지 않도록 로그만 남김
        try {
            stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        } catch (Exception e) {
            log.warn("Otoco response delete failed after cancel. otocoId={}", entity.getOtocoId(), e);
        }
        otocoCancelResponseEventPublisher.publish(OtocoCancelResponseEvent.of(entity, true, null));
    }
}
