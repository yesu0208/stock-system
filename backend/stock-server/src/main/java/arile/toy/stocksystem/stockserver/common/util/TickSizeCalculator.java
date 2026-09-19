package arile.toy.stocksystem.stockserver.common.util;

/**
 * 한국 주식시장(KRX) 호가 단위 계산 유틸.
 * 프론트엔드(AdvancedOrder.tsx, OrderPanel.tsx)의 getTickSize/getTickBaseline/
 * ceilToTick/floorToTick 로직과 동일하게 유지해야 함 — 값 불일치 시 프론트에 표시된
 * 예상 체결가와 실제 등록되는 주문가가 달라짐.
 */
public final class TickSizeCalculator {

    private TickSizeCalculator() {
    }

    public static int getTickSize(int price) {
        if (price < 2_000) return 1;
        if (price < 5_000) return 5;
        if (price < 20_000) return 10;
        if (price < 50_000) return 50;
        if (price < 200_000) return 100;
        if (price < 500_000) return 500;
        return 1_000;
    }

    public static int getTickBaseline(int price) {
        if (price < 2_000) return 0;
        if (price < 5_000) return 2_000;
        if (price < 20_000) return 5_000;
        if (price < 50_000) return 20_000;
        if (price < 200_000) return 50_000;
        if (price < 500_000) return 200_000;
        return 500_000;
    }

    /** 가장 가까운 호가 단위로 반올림 */
    public static int snapToTick(int price) {
        int clamped = Math.max(0, price);
        int tick = getTickSize(clamped);
        int baseline = getTickBaseline(clamped);
        int offset = Math.round((float) (clamped - baseline) / tick) * tick;
        return Math.max(0, baseline + offset);
    }

    /** 호가 단위로 올림 — 매수 스탑/익절가 등 "이 가격 이상" 보장이 필요한 경우 */
    public static int ceilToTick(int price) {
        int clamped = Math.max(0, price);
        int tick = getTickSize(clamped);
        int baseline = getTickBaseline(clamped);
        int offset = (int) Math.ceil((double) (clamped - baseline) / tick) * tick;
        return Math.max(0, baseline + offset);
    }

    /** 호가 단위로 내림 — 매도 스탑/손절가 등 "이 가격 이하" 보장이 필요한 경우 */
    public static int floorToTick(int price) {
        int clamped = Math.max(0, price);
        int tick = getTickSize(clamped);
        int baseline = getTickBaseline(clamped);
        int offset = (int) Math.floor((double) (clamped - baseline) / tick) * tick;
        return Math.max(0, baseline + offset);
    }
}
