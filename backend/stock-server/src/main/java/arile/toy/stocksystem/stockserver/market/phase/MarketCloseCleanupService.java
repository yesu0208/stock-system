package arile.toy.stocksystem.stockserver.market.phase;

import arile.toy.stocksystem.stockserver.autocancel.service.AutoCancelService;
import arile.toy.stocksystem.stockserver.autoorder.entity.AutoOrderEntity;
import arile.toy.stocksystem.stockserver.autoorder.service.AutoOrderService;
import arile.toy.stocksystem.stockserver.cancel.service.CancelService;
import arile.toy.stocksystem.stockserver.order.entity.OrderEntity;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.service.OtocoService;
import arile.toy.stocksystem.stockserver.otococancel.service.OtocoCancelService;
import arile.toy.stocksystem.stockserver.trailingstop.entity.TrailingStopEntity;
import arile.toy.stocksystem.stockserver.trailingstop.service.TrailingStopService;
import arile.toy.stocksystem.stockserver.trailingstopcancel.service.TrailingStopCancelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 장 마감(정규장·애프터마켓) 시 이 서버 담당 종목의 미체결 주문·미발동 자동주문·트레일링 스탑·OTOCO를 강제 취소.
 * 한 건(또는 한 종류) 정리가 실패해도 나머지 정리는 계속 진행.
 * 정리 이후의 전체 정산(settleAll)이 묶인 예약금을 풀어 주는 안전망이므로,
 * 정리 실패로 정산 단계까지 도달하지 못하는 일이 없어야 하기 때문.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCloseCleanupService {

    private final OrderService orderService;
    private final CancelService cancelService;
    private final AutoOrderService autoOrderService;
    private final AutoCancelService autoCancelService;
    private final TrailingStopService trailingStopService;
    private final TrailingStopCancelService trailingStopCancelService;
    private final OtocoService otocoService;
    private final OtocoCancelService otocoCancelService;

    /** @return 정리에 실패한 건수 (조회 실패는 1건으로 셈) */
    public int cleanUp(List<String> stockCodes) {
        int failures = 0;

        failures += cancelEach("order",
                () -> orderService.findAllUnfilledOrders(stockCodes),
                OrderEntity::getOrderId,
                cancelService::forceCancel);

        failures += cancelEach("auto-order",
                () -> autoOrderService.findAllUntriggeredAutoOrders(stockCodes),
                AutoOrderEntity::getAutoOrderId,
                autoCancelService::forceAutoCancel);

        failures += cancelEach("trailing-stop",
                () -> trailingStopService.findAllUntriggeredTrailingStops(stockCodes),
                TrailingStopEntity::getTrailingStopId,
                trailingStopCancelService::forceCancelTrailingStop);

        failures += cancelEach("otoco",
                () -> otocoService.findAllUnfinishedOtocos(stockCodes),
                OtocoEntity::getOtocoId,
                otocoCancelService::forceCancelOtoco);

        log.info("[MarketCloseCleanup] cleanup finished. stockCodes={}, failures={}", stockCodes, failures);
        return failures;
    }

    private <T> int cancelEach(String kind, Supplier<List<T>> finder,
                               Function<T, Long> idOf, Consumer<Long> forceCancel) {
        List<T> targets;
        try {
            targets = finder.get();
        } catch (Exception e) {
            log.error("[MarketCloseCleanup] failed to find {} targets.", kind, e);
            return 1;
        }

        int failures = 0;
        for (T target : targets) {
            Long id = idOf.apply(target);
            try {
                forceCancel.accept(id);
            } catch (Exception e) {
                failures++;
                log.error("[MarketCloseCleanup] failed to cancel {}. id={}", kind, id, e);
            }
        }
        return failures;
    }
}
