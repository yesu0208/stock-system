package arile.toy.stocksystem.stockserver.trailingstop.entity;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopStatus;
import arile.toy.stocksystem.stockserver.trailingstop.dto.TrailingStopType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "trailing_stops")
public class TrailingStopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trailingStopId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TrailingStopType trailingStopType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeverageRatio leverageRatio;

    @Column(nullable = false)
    private Integer orderQuantity;

    @Column(nullable = false)
    private Double stopPercent;

    /** 등록 시점의 기준가(현재가). 등록 이후 변경하지 않음. 추적 중인 기준가는 currentBasePrice에 저장. */
    @Column(nullable = false)
    private Integer basePrice;

    /** 등록 시점의 발동가. BUY 현금 예약 금액 계산 및 취소 시 환불 금액 계산의 기준이 되므로 등록 이후 변경하지 않음. */
    @Column(nullable = false)
    private Integer triggerPrice;

    /**
     * 추적 중인 기준가(최고가/최저가). 재시작 시 추적 상태를 복구하기 위해 주기적으로 저장(TrailingStopTrailPersister).
     * null이면 아직 추적 갱신이 저장되지 않은 상태로, 등록 시점 기준가(basePrice)를 사용.
     */
    private Integer currentBasePrice;

    /** 추적 중인 발동가. null이면 등록 시점 발동가(triggerPrice)를 사용. */
    private Integer currentTriggerPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TrailingStopStatus trailingStopStatus;

    @Column(nullable = false)
    private Instant orderTime;

    public static TrailingStopEntity of(String username, String stockCode, TrailingStopType trailingStopType,
                                        LeverageRatio leverageRatio, Integer orderQuantity, Double stopPercent,
                                        Integer basePrice, Integer triggerPrice, TrailingStopStatus trailingStopStatus) {
        var entity = new TrailingStopEntity();
        entity.setUsername(username);
        entity.setStockCode(stockCode);
        entity.setTrailingStopType(trailingStopType);
        entity.setLeverageRatio(leverageRatio);
        entity.setOrderQuantity(orderQuantity);
        entity.setStopPercent(stopPercent);
        entity.setBasePrice(basePrice);
        entity.setTriggerPrice(triggerPrice);
        entity.setTrailingStopStatus(trailingStopStatus);
        entity.setOrderTime(Instant.now());
        return entity;
    }

    public void changeTrailingStopStatus(TrailingStopStatus trailingStopStatus) {
        this.trailingStopStatus = trailingStopStatus;
    }

    /** 추적 중인 기준가 (저장된 추적값이 없으면 등록 시점 기준가) */
    public Integer resolveCurrentBasePrice() {
        return currentBasePrice != null ? currentBasePrice : basePrice;
    }

    /** 추적 중인 발동가 (저장된 추적값이 없으면 등록 시점 발동가) */
    public Integer resolveCurrentTriggerPrice() {
        return currentTriggerPrice != null ? currentTriggerPrice : triggerPrice;
    }
}
