package arile.toy.stocksystem.bffserver.otoco.dto;

import arile.toy.stocksystem.bffserver.leverage.dto.LeverageRatio;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OtocoRequest(
        @NotEmpty String stockCode,
        @NotNull OtocoEntryDirection entryDirection,
        @NotNull @Positive Integer orderQuantity,
        @NotNull @Positive Integer entryTriggerPrice,
        @NotNull OtocoExitMode tpMode,
        Integer tpPrice,
        Double tpPct,
        @NotNull OtocoExitMode slMode,
        Integer slPrice,
        Double slPct,
        LeverageRatio leverageRatio
) {
    /** 익절·손절 퍼센트 허용 범위. 일일 가격제한폭(±30%)을 넘는 폭은 당일(장 마감 시 강제 취소) 안에 발동할 수 없음 */
    private static final double MIN_PCT = 0.1;
    private static final double MAX_PCT = 30.0;

    public LeverageRatio leverageRatioOrDefault() {
        return leverageRatio == null ? LeverageRatio.SPOT : leverageRatio;
    }

    /**
     * 익절 조건 검증. 모드에 해당하는 값이 있어야 하며
     * PRICE는 진입가보다 높아야 하고, PCT는 허용 범위 안이어야 한함.
     * (모드·진입가 누락은 @NotNull이 따로 검증하므로 여기서는 통과시킴)
     */
    @AssertTrue(message = "익절가는 진입가보다 높아야 하며, 퍼센트는 0.1~30% 사이여야 합니다.")
    public boolean isTpValid() {
        if (tpMode == null || entryTriggerPrice == null) {
            return true;
        }
        return switch (tpMode) {
            case PRICE -> tpPrice != null && tpPrice > entryTriggerPrice;
            case PCT -> isPctInRange(tpPct);
        };
    }

    /**
     * 손절 조건 검증. 모드에 해당하는 값이 있어야 하며
     * PRICE는 0보다 크고 진입가보다 낮아야 하고, PCT는 허용 범위 안이어야 함.
     * (손절가 0 이하는 주가가 도달할 수 없어 영원히 발동하지 않음)
     */
    @AssertTrue(message = "손절가는 0원보다 크고 진입가보다 낮아야 하며, 퍼센트는 0.1~30% 사이여야 합니다.")
    public boolean isSlValid() {
        if (slMode == null || entryTriggerPrice == null) {
            return true;
        }
        return switch (slMode) {
            case PRICE -> slPrice != null && slPrice > 0 && slPrice < entryTriggerPrice;
            case PCT -> isPctInRange(slPct);
        };
    }

    private static boolean isPctInRange(Double pct) {
        return pct != null && pct >= MIN_PCT && pct <= MAX_PCT;
    }
}
