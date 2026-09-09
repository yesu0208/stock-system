import { createContext, useContext, useEffect, useState, useCallback } from "react"
import type { ReactNode } from "react"
import type { StockInfo } from "../types/stock"
import type { StockInfoResponse } from "../types/stockInfo"
import type { StockDetailExtraResponse } from "../types/stockDetail"
import { STOCKS, isSupportedStock } from "../data/stocks"
import { getStockInfo, getStockDetailExtra } from "../api/stocks"

type StockContextType = {
    selectedStock: StockInfo
    setSelectedStock: (s: StockInfo) => void
    info: StockInfoResponse | null
    detailExtra: StockDetailExtraResponse | null
    loading: boolean
}

const StockContext = createContext<StockContextType | undefined>(undefined)

export function StockProvider({ children }: { children: ReactNode }) {
    // STOCKS 배열의 첫 항목은 항상 지원 종목이 오도록 순서를 유지한다.
    const [selectedStock, setSelectedStockState] = useState<StockInfo>(STOCKS[0])
    const [info, setInfo] = useState<StockInfoResponse | null>(null)
    const [detailExtra, setDetailExtra] = useState<StockDetailExtraResponse | null>(null)
    const [loading, setLoading] = useState(true)

    /**
     * 미지원 종목으로의 전환 시도는 조용히 무시한다.
     * 이 앱에서 "선택된 종목(selectedStock)"은 항상 지원 종목이라는
     * 불변식을 이 지점 하나에서 보장한다. RealtimeContext,
     * ChartDataContext, OrderPanel 등은 이 불변식을 신뢰하고
     * 별도의 지원 여부 확인 없이 동작한다.
     */
    const setSelectedStock = useCallback((stock: StockInfo) => {
        if (!isSupportedStock(stock.code)) {
            console.warn(`[StockContext] 미지원 종목이라 선택할 수 없습니다: ${stock.code} ${stock.name}`)
            return
        }
        setSelectedStockState(stock)
    }, [])

    useEffect(() => {
        let cancelled = false
        setLoading(true)

        Promise.all([
            getStockInfo(selectedStock.code),
            getStockDetailExtra(selectedStock.code),
        ])
            .then(([infoData, detailData]) => {
                if (cancelled) return
                setInfo(infoData)
                setDetailExtra(detailData)
            })
            .catch((e) => {
                console.error("종목 조회 실패", e)
                if (!cancelled) {
                    setInfo(null)
                    setDetailExtra(null)
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false)
            })

        return () => {
            cancelled = true
        }
    }, [selectedStock.code])

    return (
        <StockContext.Provider value={{ selectedStock, setSelectedStock, info, detailExtra, loading }}>
            {children}
        </StockContext.Provider>
    )
}

export function useStock() {
    const ctx = useContext(StockContext)
    if (!ctx) throw new Error("useStock must be used within StockProvider")
    return ctx
}
