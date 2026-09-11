import { STOCKS } from "../main/data/stocks";

export function isRealtimeStock(code: string): boolean {
    return STOCKS.find(s => s.code === code)?.realtimeSupported ?? false;
}

/* 방향 타입  */
export type Direction = "up" | "down" | "flat";

export const DIRECTION_CLASS: Record<Direction, string> = {
    up:   "price-up",
    down: "price-down",
    flat: "price-flat",
};

export const DIRECTION_ARROW: Record<Direction, string> = {
    up:   "▲",
    down: "▼",
    flat: "─",
};

/* 방향 판별 */
export function directionFromDiff(diff: number | null | undefined): Direction {
    if (diff === null || diff === undefined) return "flat";
    if (diff > 0) return "up";
    if (diff < 0) return "down";
    return "flat";
}

export function directionFromString(dir: string | null | undefined): Direction {
    if (dir === "상승") return "up";
    if (dir === "하락") return "down";
    return "flat";
}

/* 숫자 변환 */
export function toNumber(val: number | string | null | undefined): number | null {
    if (val === null || val === undefined || val === "") return null;
    const n = typeof val === "string" ? parseFloat(val.replace(/,/g, "")) : val;
    return isNaN(n) ? null : n;
}

/* 숫자 포맷 */
export function fmtNumber(val: number | string | null | undefined): string {
    if (val === null || val === undefined || val === "") return "-";
    const n = typeof val === "string" ? parseFloat(val.replace(/,/g, "")) : val;
    if (isNaN(n)) return "-";
    return n.toLocaleString("ko-KR");
}

/* 거래대금: 백만 단위 절사 */
export function fmtTradingValue(val: number | string | null | undefined): string {
    if (val === null || val === undefined || val === "") return "-";
    const n = typeof val === "string" ? parseFloat(val.replace(/,/g, "")) : val;
    if (isNaN(n)) return "-";
    return Math.floor(n / 1_000_000).toLocaleString();
}

export function fmtRate(val: number | string | null | undefined): string {
    if (val === null || val === undefined || val === "") return "-";
    if (typeof val === "number") {
        return `${val > 0 ? "+" : ""}${val.toFixed(2)}%`;
    }
    return val;
}

/*  prevClose 기준 가격/등락/등락률 계산  */
export interface PriceStats {
    direction: Direction;
    price:     string;
    diffText:  string;
    rateText:  string;
}

export function calcFromPrevClose(price: number, prev: number): PriceStats {
    const diff = price - prev;
    const rate = prev !== 0 ? (diff / prev) * 100 : 0;
    const direction: Direction = diff > 0 ? "up" : diff < 0 ? "down" : "flat";
    return {
        direction,
        price:    fmtNumber(price),
        diffText: `${DIRECTION_ARROW[direction]} ${fmtNumber(Math.abs(diff))}`,
        rateText: fmtRate(rate),
    };
}

/* 시가/고가/저가 전용 계산 헬퍼
 * openPrice/highPrice/lowPrice가 "0"으로 내려오는 경우는
 * 실제 0원이 아니라 "값 없음(장 시작 전 등)"을 의미하므로
 * prevClose와 비교하지 않고 direction: flat, rateText: "" 로 고정 */
function calcOhlc(rawPrice: number | string | null | undefined, prevClose: number): PriceStats {
    const price = toNumber(rawPrice) ?? 0;
    if (price === 0) {
        return {
            direction: "flat",
            price:     fmtNumber(price),
            diffText:  "-",
            rateText:  "",
        };
    }
    return calcFromPrevClose(price, prevClose);
}



/* 색상 판별 유틸 */

export function priceDirection(diff: number): Direction {
    if (diff > 0) return "up";
    if (diff < 0) return "down";
    return "flat";
}

export function ohlcDirection(
    targetPrice: number,
    curPrice:    number,
    diff:        number,
): Direction {
    const prevClose = curPrice - diff;
    if (targetPrice > prevClose) return "up";
    if (targetPrice < prevClose) return "down";
    return "flat";
}

/* priceTick / detail -> 통합 stats 계산 */
export interface StockStats {
    cur:          PriceStats;
    open:         PriceStats;
    high:         PriceStats;
    low:          PriceStats;
    volume:       string;
    tradingValue: string;
}

export function calcStockStats(
    isRealtime: boolean,
    priceTick:  any | null,
    detail:     any | null,
): StockStats | null {
    const prevClose = toNumber(
        isRealtime ? priceTick?.prevClosePrice : detail?.prevClosePrice
    );
    if (prevClose === null) return null;

    if (isRealtime && priceTick) {
        const curPrice = toNumber(priceTick.curPrice) ?? 0;

        return {
            cur:  calcFromPrevClose(curPrice, prevClose),
            open: calcOhlc(priceTick.startPrice, prevClose),
            high: calcOhlc(priceTick.highPrice,  prevClose),
            low:  calcOhlc(priceTick.lowPrice,   prevClose),
            volume:       fmtNumber(priceTick.totalTradingVolume),
            tradingValue: fmtTradingValue(priceTick.totalTradingValue),
        };
    }

    if (!isRealtime && detail) {
        const curPrice = toNumber(detail.currentPrice) ?? 0;

        return {
            cur:  calcFromPrevClose(curPrice, prevClose),
            open: calcOhlc(detail.openPrice, prevClose),
            high: calcOhlc(detail.highPrice, prevClose),
            low:  calcOhlc(detail.lowPrice,  prevClose),
            volume:       fmtNumber(detail.volume),
            tradingValue: fmtTradingValue(detail.tradingValue),
        };
    }

    return null;
}
