import { createContext, useContext, useEffect, useState } from "react"
import type { ReactNode } from "react"
import type { StockInfo } from "../types/stock"
import type { StockInfoResponse } from "../types/stockInfo"
import type { StockDetailExtraResponse } from "../types/stockDetail"
import { STOCKS } from "../data/stocks"
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
    const [selectedStock, setSelectedStock] = useState<StockInfo>(STOCKS[0])
    const [info, setInfo] = useState<StockInfoResponse | null>(null)
    const [detailExtra, setDetailExtra] = useState<StockDetailExtraResponse | null>(null)
    const [loading, setLoading] = useState(true)

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
