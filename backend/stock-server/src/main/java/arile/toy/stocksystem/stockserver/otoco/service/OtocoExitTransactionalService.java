package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.event.StockServerOrderRequestEvent;
import arile.toy.stocksystem.stockserver.order.service.OrderService;
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
public class OtocoExitTransactionalService {

    private final OtocoRepository otocoRepository;
    private final OrderService orderService;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final AccountApiClient accountApiClient;

    @Transactional
    public void triggerExit(OtocoDto dto, OtocoLeg leg) {

        OtocoEntity entity = otocoRepository.findByIdForUpdate(dto.otocoId())
                .orElseThrow(() -> new IllegalArgumentException("otoco not found"));

        if (entity.getOtocoStatus() != OtocoStatus.WAITING_EXIT) {
            return;
        }

        Integer exitPrice = leg == OtocoLeg.TAKE_PROFIT ? entity.getTpTriggerPrice() : entity.getSlTriggerPrice();

        StockServerOrderRequestEvent event = StockServerOrderRequestEvent.fromOtocoExit(dto, exitPrice, leg);

        try {
            orderService.registerOrder(event, true);
        } catch (Exception e) {
            log.error("Otoco exit -> order registration failed. otocoId={}, username={}, leg={}",
                    dto.otocoId(), dto.username(), leg, e);
            compensateFailedExit(entity, leg);
            return;
        }

        entity.markCompleted(leg);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());

        otocoResponseEventPublisher.publishExitTriggered(OtocoDto.fromEntity(entity), leg);
    }

    private void compensateFailedExit(OtocoEntity entity, OtocoLeg leg) {

        boolean refunded = entity.getLeverageRatio().isSpot()
                ? accountApiClient.refundReservedStock(entity.getUsername(), entity.getStockCode(), entity.getOrderQuantity())
                : accountApiClient.refundReservedLeverageStock(entity.getUsername(), entity.getStockCode(),
                entity.getLeverageRatio().name(), entity.getOrderQuantity());

        if (!refunded) {
            log.error("CRITICAL: Otoco exit trigger compensation refund FAILED. " +
                            "Manual intervention required. otocoId={}, username={}, leg={}",
                    entity.getOtocoId(), entity.getUsername(), leg);
        }

        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        otocoResponseEventPublisher.publishExitFailed(OtocoDto.fromEntity(entity), OtocoResultCode.INTERNAL_ERROR);
    }
}
