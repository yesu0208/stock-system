package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoResultCode;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
import arile.toy.stocksystem.stockserver.useraccount.client.AccountApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtocoEntryFillService implements OtocoOrderLifecycleListener {

    private final OtocoRepository otocoRepository;
    private final OtocoExitBookRegistry otocoExitBookRegistry;
    private final OtocoResponseEventPublisher otocoResponseEventPublisher;
    private final StockServerOtocoResponseRepository stockServerOtocoResponseRepository;
    private final AccountApiClient accountApiClient;

    @Override
    public void onOrderFilled(Long orderId) {

        Optional<OtocoEntity> optionalOtoco = otocoRepository.findByEntryOrderIdForUpdate(orderId);
        if (optionalOtoco.isEmpty()) {
            return; // OTOCO와 무관한 일반 주문
        }

        OtocoEntity entity = optionalOtoco.get();
        if (entity.getOtocoStatus() != OtocoStatus.ENTRY_ORDER_PLACED) {
            return; // 이미 처리됨(중복 호출 방지)
        }

        LeverageRatio leverageRatio = entity.getLeverageRatio();

        boolean reserved = leverageRatio.isSpot()
                ? accountApiClient.reserveStock(entity.getUsername(), entity.getStockCode(), entity.getOrderQuantity())
                : accountApiClient.reserveLeverageStock(entity.getUsername(), entity.getStockCode(),
                leverageRatio.name(), entity.getOrderQuantity());

        if (!reserved) {
            // 방금 체결로 보유하게 된 수량에 대한 예약이므로 사실상 실패할 수 없지만, 방어적으로 처리
            // 포지션 자체는 유지되고 OCO 추적만 포기
            log.error("CRITICAL: OTOCO exit stock reservation failed right after entry fill. otocoId={}, username={}",
                    entity.getOtocoId(), entity.getUsername());
            entity.changeStatus(OtocoStatus.CANCELED);
            otocoRepository.save(entity);
            stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
            otocoResponseEventPublisher.publishEntryFailed(OtocoDto.fromEntity(entity), OtocoResultCode.ENTRY_FAILED);
            return;
        }

        entity.changeStatus(OtocoStatus.WAITING_EXIT);
        otocoRepository.save(entity);

        otocoExitBookRegistry.register(OtocoDto.fromEntity(entity));

        stockServerOtocoResponseRepository.update(entity.getUsername(), entity.getOtocoId(),
                StockServerOtocoResponseMessage.fromEntity(entity));

        otocoResponseEventPublisher.publishEntryFilled(OtocoDto.fromEntity(entity));
    }

    @Override
    public void onOrderCanceled(Long orderId) {

        Optional<OtocoEntity> optionalOtoco = otocoRepository.findByEntryOrderIdForUpdate(orderId);
        if (optionalOtoco.isEmpty()) {
            return;
        }

        OtocoEntity entity = optionalOtoco.get();
        if (entity.getOtocoStatus() != OtocoStatus.ENTRY_ORDER_PLACED) {
            return;
        }

        // 표준 취소 흐름(CancelService)이 이미 예약 현금을 환불했으므로 여기서는 상태만 정리
        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        otocoResponseEventPublisher.publishEntryFailed(OtocoDto.fromEntity(entity), OtocoResultCode.ENTRY_FAILED);
    }
}
