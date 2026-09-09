export type Direction = "up" | "down" | "flat"

export const DIRECTION_CLASS: Record<Direction, string> = {
    up: "price-up",
    down: "price-down",
    flat: "price-flat",
}

export const DIRECTION_ARROW: Record<Direction, string> = {
    up: "▲",
    down: "▼",
    flat: "─",
}

export function toNumber(val: number | string | null | undefined): number | null {
    if (val === null || val === undefined || val === "") return null
    const n = typeof val === "string" ? parseFloat(val.replace(/,/g, "")) : val
    return isNaN(n) ? null : n
}

export function fmtNumber(val: number | string | null | undefined): string {
    const n = toNumber(val)
    if (n === null) return "-"
    return n.toLocaleString("ko-KR")
}

export function fmtTradingValue(val: number | string | null | undefined): string {
    const n = toNumber(val)
    if (n === null) return "-"
    return Math.floor(n / 1_000_000).toLocaleString()
}

export function fmtRate(val: number | string | null | undefined): string {
    if (val === null || val === undefined || val === "") return "-"
    if (typeof val === "number") {
        return `${val > 0 ? "+" : ""}${val.toFixed(2)}%`
    }
    return val
}

export interface PriceStats {
    direction: Direction
    price: string
    diffText: string
    rateText: string
}

export function calcFromPrevClose(price: number, prev: number): PriceStats {
    const diff = price - prev
    const rate = prev !== 0 ? (diff / prev) * 100 : 0
    const direction: Direction = diff > 0 ? "up" : diff < 0 ? "down" : "flat"
    return {
        direction,
        price: fmtNumber(price),
        diffText: `${DIRECTION_ARROW[direction]} ${fmtNumber(Math.abs(diff))}`,
        rateText: fmtRate(rate),
    }
}

function calcOhlc(rawPrice: number | string | null | undefined, prevClose: number): PriceStats {
    const price = toNumber(rawPrice) ?? 0
    if (price === 0) {
        return { direction: "flat", price: fmtNumber(price), diffText: "-", rateText: "" }
    }
    return calcFromPrevClose(price, prevClose)
}

export interface StockStats {
    cur: PriceStats
    open: PriceStats
    high: PriceStats
    low: PriceStats
    volume: string
    tradingValue: string
}

/**
 * 실시간 체결 틱 하나만으로 화면에 필요한 모든 통계를 계산
 * 호출 시점의 종목은 항상 지원 종목(StockContext 가드를 통과한 selectedStock)이므로 종목별 분기가 필요 없음.
 */
export function calcStockStats(priceTick: import("../types/realtime").TradePriceTickMessage | null): StockStats | null {
    if (!priceTick) return null
    const prevClose = priceTick.prevClosePrice

    return {
        cur: calcFromPrevClose(priceTick.curPrice, prevClose),
        open: calcOhlc(priceTick.startPrice, prevClose),
        high: calcOhlc(priceTick.highPrice, prevClose),
        low: calcOhlc(priceTick.lowPrice, prevClose),
        volume: fmtNumber(priceTick.totalTradingVolume),
        tradingValue: fmtTradingValue(priceTick.totalTradingValue),
    }
}
