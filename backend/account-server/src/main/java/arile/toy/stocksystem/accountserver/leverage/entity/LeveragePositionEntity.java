package arile.toy.stocksystem.accountserver.leverage.entity;

import arile.toy.stocksystem.accountserver.leverage.dto.LeverageRatio;
import arile.toy.stocksystem.accountserver.leverage.dto.MarginStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "leverage_positions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "stockCode", "leverageRatio"}))
public class LeveragePositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leveragePositionId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LeverageRatio leverageRatio;

    /** 보유 수량 */
    @Column(nullable = false)
    private Integer quantity;

    /** 매도 가능 수량 (매도 주문 예약분 차감 후) */
    @Column(nullable = false)
    private Integer availableQuantity;

    /** 매수금액 (레버리지 적용된 전체 포지션 크기, 현물의 amount와 동일 개념) */
    @Column(nullable = false)
    private Long purchaseAmount;

    /** 대출금(신용융자금) : 매일 이자만큼 증가, 부분매도 시 비례 상환 */
    @Column(nullable = false)
    private Long loanAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MarginStatus marginStatus;

    /** 마진콜 발생일 (D일). NORMAL 상태면 null */
    private LocalDate marginCallDate;

    /**
     * 마지막으로 이자가 청구된 날짜(달력일 기준, 포함).
     * 이자는 연이율/365로 설계되어 있어 주말·휴장일에도 계속 발생해야 하므로,
     * 배치 실행 시점에 "오늘 - 이 날짜"만큼의 경과일수를 한 번에 청구한 뒤 오늘로 갱신.
     * 포지션 최초 생성일에는 아직 하루도 지나지 않았으므로 오늘 날짜로 초기화.
     */
    @Column(nullable = false)
    private LocalDate lastInterestChargedDate;

    @Column(nullable = false)
    private Instant createdDateTime;

    @Column(nullable = false)
    private Instant updatedDateTime;

    public static LeveragePositionEntity of(String username, String stockCode, LeverageRatio leverageRatio,
                                            int quantity, long purchaseAmount) {
        var entity = new LeveragePositionEntity();
        entity.setUsername(username);
        entity.setStockCode(stockCode);
        entity.setLeverageRatio(leverageRatio);
        entity.setQuantity(quantity);
        entity.setAvailableQuantity(quantity);
        entity.setPurchaseAmount(purchaseAmount);
        entity.setLoanAmount(leverageRatio.calculateLoanAmount(purchaseAmount));
        entity.setMarginStatus(MarginStatus.NORMAL);
        entity.setLastInterestChargedDate(LocalDate.now());
        return entity;
    }

    /** 매수 체결 시 기존 포지션에 추가 매수분 합산 (현물 UserStockEntity의 buy 패턴과 동일) */
    public void addPurchase(int additionalQuantity, long additionalPurchaseAmount, long additionalLoanAmount) {
        this.quantity += additionalQuantity;
        this.availableQuantity += additionalQuantity;
        this.purchaseAmount += additionalPurchaseAmount;
        this.loanAmount += additionalLoanAmount;
    }

    /** 매도 체결 시 비례 상환 (현물의 soldAmount = prevAmount * executable / prevQuantity 패턴과 동일하게 대출금도 비례 차감) */
    public long reduceBySell(int soldQuantity) {
        long soldPurchaseAmount = this.purchaseAmount * soldQuantity / this.quantity;
        long repaidLoanAmount = this.loanAmount * soldQuantity / this.quantity;

        this.quantity -= soldQuantity;
        this.purchaseAmount -= soldPurchaseAmount;
        this.loanAmount -= repaidLoanAmount;

        return repaidLoanAmount;
    }

    public void changeMarginStatus(MarginStatus status, LocalDate marginCallDate) {
        this.marginStatus = status;
        this.marginCallDate = marginCallDate;
    }

    /** 이자 청구 배치가 청구 완료 후 기준일을 오늘로 갱신할 때 사용 */
    public void markInterestChargedThrough(LocalDate date) {
        this.lastInterestChargedDate = date;
    }

    public boolean isEmpty() {
        return this.quantity == 0;
    }

    @PrePersist
    private void prePersist() {
        this.createdDateTime = Instant.now();
        this.updatedDateTime = Instant.now();
        if (this.lastInterestChargedDate == null) {
            this.lastInterestChargedDate = LocalDate.now();
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedDateTime = Instant.now();
    }
}
