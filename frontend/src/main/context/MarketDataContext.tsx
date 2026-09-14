import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import type { MarketMainResponse } from '../../types/marketMain'
import type { ExchangeRateDto } from '../../types/exchangeRate'

interface GlobalMarketResponse {
    exchangeRates: ExchangeRateDto[]
}

interface MarketDataContextValue {
    marketMain: MarketMainResponse | null
    exchangeRates: ExchangeRateDto[]
}

const MarketDataContext = createContext<MarketDataContextValue | null>(null)

export function MarketDataProvider({ children }: { children: ReactNode }) {
    const [marketMain, setMarketMain] = useState<MarketMainResponse | null>(null)
    const [exchangeRates, setExchangeRates] = useState<ExchangeRateDto[]>([])
    const { subscribeDestination } = useRealtime()

    useEffect(() => {
        return subscribeDestination('/sub/market/main', (data: MarketMainResponse) => {
            setMarketMain(data)
        })
    }, [subscribeDestination])

    useEffect(() => {
        return subscribeDestination('/sub/market/global', (data: GlobalMarketResponse) => {
            setExchangeRates(data.exchangeRates ?? [])
        })
    }, [subscribeDestination])

    return (
        <MarketDataContext.Provider value={{ marketMain, exchangeRates }}>
            {children}
        </MarketDataContext.Provider>
    )
}

export function useMarketData() {
    const ctx = useContext(MarketDataContext)
    if (!ctx) throw new Error('useMarketData는 MarketDataProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
