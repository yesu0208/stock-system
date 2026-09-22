import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import { useStock } from './StockContext'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import type { BidAskPriceTickMessage } from '../../types/bidAskPriceTickMessage'

interface OrderBookData {
    asks: { price: number; quantity: number }[]
    bids: { price: number; quantity: number }[]
}

interface StockRealtimeContextValue {
    priceTick: TradePriceTickMessage | null
    orderbook: OrderBookData | null
    connected: boolean
}

const StockRealtimeContext = createContext<StockRealtimeContextValue | null>(null)

export function StockRealtimeProvider({ children }: { children: ReactNode }) {
    const { selectedStock } = useStock()
    const { subscribeStock, connected } = useRealtime()

    const [priceTick, setPriceTick] = useState<TradePriceTickMessage | null>(null)
    const [orderbook, setOrderbook] = useState<OrderBookData | null>(null)

    const [appliedCode, setAppliedCode] = useState(selectedStock.code)
    if (appliedCode !== selectedStock.code) {
        setAppliedCode(selectedStock.code)
        setPriceTick(null)
        setOrderbook(null)
    }

    useEffect(() => {
        return subscribeStock(selectedStock.code, (tick: any) => {
            if (tick.tickMessageType === 'TRADEPRICE') {
                setPriceTick(tick as TradePriceTickMessage)
            }
            if (tick.tickMessageType === 'BIDASKPRICE') {
                const msg = tick as BidAskPriceTickMessage
                setOrderbook({ asks: msg.asks ?? [], bids: msg.bids ?? [] })
            }
        })
    }, [selectedStock.code, subscribeStock])

    return (
        <StockRealtimeContext.Provider value={{ priceTick, orderbook, connected }}>
            {children}
        </StockRealtimeContext.Provider>
    )
}

export function useStockRealtime() {
    const ctx = useContext(StockRealtimeContext)
    if (!ctx) throw new Error('useStockRealtime은 StockRealtimeProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
