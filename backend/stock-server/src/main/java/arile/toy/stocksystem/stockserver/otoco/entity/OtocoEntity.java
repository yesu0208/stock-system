package arile.toy.stocksystem.stockserver.otoco.entity;

import arile.toy.stocksystem.stockserver.order.dto.LeverageRatio;
import arile.toy.stocksystem.stockserver.otoco.dto.*;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "otocos")
public class OtocoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long otocoId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtocoEntryDirection entryDirection;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeverageRatio leverageRatio;

    @Column(nullable = false)
    private Integer orderQuantity;

    @Column(nullable = false)
    private Integer entryTriggerPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtocoExitMode tpMode;

    private Integer tpPrice;
    private Double tpPct;

    /** 등록 시점에 entryTriggerPrice 기준으로 미리 계산해 확정 (체결가 기준이 아님) */
    @Column(nullable = false)
    private Integer tpTriggerPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtocoExitMode slMode;

    private Integer slPrice;
    private Double slPct;

    @Column(nullable = false)
    private Integer slTriggerPrice;

    /** 진입 트리거 이후 실제 등록된 주문의 id. 체결/취소 훅에서 이 값으로 역참조 */
    private Long entryOrderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OtocoStatus otocoStatus;

    /** COMPLETED 상태일 때만 값이 채워짐 */
    @Enumerated(EnumType.STRING)
    private OtocoLeg completedLeg;

    @Column(nullable = false)
    private Instant orderTime;

    public static OtocoEntity of(String username, String stockCode, OtocoEntryDirection entryDirection,
                                 LeverageRatio leverageRatio, Integer orderQuantity, Integer entryTriggerPrice,
                                 OtocoExitMode tpMode, Integer tpPrice, Double tpPct, Integer tpTriggerPrice,
                                 OtocoExitMode slMode, Integer slPrice, Double slPct, Integer slTriggerPrice) {
        var entity = new OtocoEntity();
        entity.setUsername(username);
        entity.setStockCode(stockCode);
        entity.setEntryDirection(entryDirection);
        entity.setLeverageRatio(leverageRatio);
        entity.setOrderQuantity(orderQuantity);
        entity.setEntryTriggerPrice(entryTriggerPrice);
        entity.setTpMode(tpMode);
        entity.setTpPrice(tpPrice);
        entity.setTpPct(tpPct);
        entity.setTpTriggerPrice(tpTriggerPrice);
        entity.setSlMode(slMode);
        entity.setSlPrice(slPrice);
        entity.setSlPct(slPct);
        entity.setSlTriggerPrice(slTriggerPrice);
        entity.setOtocoStatus(OtocoStatus.WAITING_ENTRY);
        entity.setOrderTime(Instant.now());
        return entity;
    }

    public void changeStatus(OtocoStatus status) {
        this.otocoStatus = status;
    }

    public void markCompleted(OtocoLeg leg) {
        this.otocoStatus = OtocoStatus.COMPLETED;
        this.completedLeg = leg;
    }
}
