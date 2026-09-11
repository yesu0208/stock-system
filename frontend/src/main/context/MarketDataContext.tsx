import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import { getStockClient } from '../../api/stompClient'
import { tokenStorage } from '../../utils/token'
import type { MarketMainResponse } from '../../types/marketMain'

interface MarketDataContextValue {
    marketMain: MarketMainResponse | null
}

const MarketDataContext = createContext<MarketDataContextValue | null>(null)

export function MarketDataProvider({ children }: { children: ReactNode }) {
    const [marketMain, setMarketMain] = useState<MarketMainResponse | null>(null)

    useEffect(() => {
        if (!tokenStorage.get()) return

        const client = getStockClient()
        let sub: StompSubscription | undefined

        client.onConnect = () => {
            sub = client.subscribe('/sub/market/main', (message) => {
                const data: MarketMainResponse = JSON.parse(message.body)
                setMarketMain(data)
            })
        }

        client.activate()

        return () => {
            sub?.unsubscribe()
            client.deactivate()
        }
    }, [])

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
