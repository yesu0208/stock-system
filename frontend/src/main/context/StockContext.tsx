import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { STOCKS } from '../data/stocks'
import type { StockInfo } from '../types/stock'

const DEFAULT_STOCK_CODE = '005930'

function resolveDefaultStock(): StockInfo {
    const preferred = STOCKS.find(s => s.code === DEFAULT_STOCK_CODE)
    if (preferred) return preferred

    const firstRealtime = STOCKS.find(s => s.realtimeSupported)
    if (firstRealtime) return firstRealtime

    return STOCKS[0]
}

interface StockContextValue {
    selectedStock: StockInfo
    setSelectedStock: (stock: StockInfo) => void
}

const StockContext = createContext<StockContextValue | null>(null)

export function StockProvider({ children }: { children: ReactNode }) {
    const [selectedStock, setSelectedStock] = useState<StockInfo>(() => resolveDefaultStock())

    const value = useMemo(() => ({ selectedStock, setSelectedStock }), [selectedStock])

    return <StockContext.Provider value={value}>{children}</StockContext.Provider>
}

export function useStock() {
    const ctx = useContext(StockContext)
    if (!ctx) throw new Error('useStock은 StockProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
