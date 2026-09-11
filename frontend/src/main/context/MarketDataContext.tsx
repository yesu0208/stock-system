import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import type { MarketMainResponse } from '../../types/marketMain'

interface MarketDataContextValue {
    marketMain: MarketMainResponse | null
}

const MarketDataContext = createContext<MarketDataContextValue | null>(null)

export function MarketDataProvider({ children }: { children: ReactNode }) {
    const [marketMain, setMarketMain] = useState<MarketMainResponse | null>(null)
    const { subscribeDestination } = useRealtime()

    useEffect(() => {
        return subscribeDestination('/sub/market/main', (data: MarketMainResponse) => {
            setMarketMain(data)
        })
    }, [subscribeDestination])

    return (
        <MarketDataContext.Provider value={{ marketMain }}>
            {children}
        </MarketDataContext.Provider>
    )
}

export function useMarketData() {
    const ctx = useContext(MarketDataContext)
    if (!ctx) throw new Error('useMarketData는 MarketDataProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
