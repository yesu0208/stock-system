package arile.toy.stocksystem.stockserver.otoco.service;

import arile.toy.stocksystem.stockserver.otoco.dto.OtocoDto;
import arile.toy.stocksystem.stockserver.otoco.dto.OtocoStatus;
import arile.toy.stocksystem.stockserver.otoco.dto.StockServerOtocoResponseMessage;
import arile.toy.stocksystem.stockserver.otoco.entity.OtocoEntity;
import arile.toy.stocksystem.stockserver.otoco.event.publisher.OtocoResponseEventPublisher;
import arile.toy.stocksystem.stockserver.otoco.registry.OtocoExitBookRegistry;
import arile.toy.stocksystem.stockserver.otoco.repository.OtocoRepository;
import arile.toy.stocksystem.stockserver.otoco.repository.StockServerOtocoResponseRepository;
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

        // 청산용 주식은 여기서 예약하지 않음: 이 시점(체결 트랜잭션 내부)에는 체결분이 아직
        // account-server에 반영되지 않아 보유 수량이 없음. 청산 발동 시 매도 주문 등록 과정에서 예약함.
        entity.changeStatus(OtocoStatus.WAITING_EXIT);
        otocoRepository.save(entity);

        otocoExitBookRegistry.register(OtocoDto.fromEntity(entity));

        // 완전체결되었으므로 잔량 개념이 사라짐 — 캐시에도 entryRemainingQuantity 없이 저장
        // 체결 트랜잭션 안에서 호출되므로 부가 작업 실패가 체결 롤백으로 번지지 않도록 로그만 남김
        try {
            stockServerOtocoResponseRepository.update(entity.getUsername(), entity.getOtocoId(),
                    StockServerOtocoResponseMessage.fromEntity(entity));
        } catch (Exception e) {
            log.warn("Otoco response update failed after entry fill. otocoId={}", entity.getOtocoId(), e);
        }

        otocoResponseEventPublisher.publishEntryFilled(OtocoDto.fromEntity(entity));
    }

    //  진입 주문이 부분체결될 때마다(완전체결 전) TradeExecutionService로부터 호출됨
    @Override
    public void onOrderPartiallyFilled(Long orderId, int remainingQuantity) {

        Optional<OtocoEntity> optionalOtoco = otocoRepository.findByEntryOrderIdForUpdate(orderId);
        if (optionalOtoco.isEmpty()) {
            return; // OTOCO와 무관한 일반 주문
        }

        OtocoEntity entity = optionalOtoco.get();
        if (entity.getOtocoStatus() != OtocoStatus.ENTRY_ORDER_PLACED) {
            return; // 이미 취소/완전체결 등으로 상태가 바뀐 경우 — 오작동 방지
        }

        try {
            stockServerOtocoResponseRepository.update(entity.getUsername(), entity.getOtocoId(),
                    StockServerOtocoResponseMessage.fromEntity(entity, remainingQuantity));
        } catch (Exception e) {
            log.warn("Otoco response update failed after entry partial fill. otocoId={}", entity.getOtocoId(), e);
        }

        otocoResponseEventPublisher.publishEntryPartiallyFilled(OtocoDto.fromEntity(entity, remainingQuantity));
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

        entity.changeStatus(OtocoStatus.CANCELED);
        otocoRepository.save(entity);

        try {
            stockServerOtocoResponseRepository.delete(entity.getUsername(), entity.getOtocoId());
        } catch (Exception e) {
            log.warn("Otoco response delete failed after entry cancel. otocoId={}", entity.getOtocoId(), e);
        }

        otocoResponseEventPublisher.publishEntryCanceled(OtocoDto.fromEntity(entity));
    }
}
