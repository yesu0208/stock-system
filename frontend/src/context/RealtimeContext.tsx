import { createContext, useContext, useEffect, useState } from "react"
import type { ReactNode } from "react"
import type { IMessage } from "@stomp/stompjs"
import { onStockConnect, getStockClient } from "../api/stompClient"
import { useStock } from "./StockContext"
import type {
    BidAskPriceTickMessage,
    TradePriceTickMessage,
    StockDetailTickMessage,
} from "../types/realtime"

type RealtimeContextType = {
    priceTick: TradePriceTickMessage | null
    orderbook: BidAskPriceTickMessage | null
    detailTick: StockDetailTickMessage | null
    connected: boolean
}

const RealtimeContext = createContext<RealtimeContextType | undefined>(undefined)

export function RealtimeProvider({ children }: { children: ReactNode }) {
    // selectedStock은 StockContext의 가드를 통과한 값만 들어오므로 여기서 다시 지원 여부를 확인할 필요가 없음
    const { selectedStock } = useStock()

    const [priceTick, setPriceTick] = useState<TradePriceTickMessage | null>(null)
    const [orderbook, setOrderbook] = useState<BidAskPriceTickMessage | null>(null)
    const [detailTick, setDetailTick] = useState<StockDetailTickMessage | null>(null)
    const [connected, setConnected] = useState(false)

    useEffect(() => {
        if (!selectedStock?.code) return
        const code = selectedStock.code

        setPriceTick(null)
        setOrderbook(null)
        setDetailTick(null)
        setConnected(false)

        const stop = onStockConnect(() => {
            const client = getStockClient()

            const sub = client.subscribe(`/sub/stock/${code}`, (msg: IMessage) => {
                const data = JSON.parse(msg.body)
                switch (data.tickMessageType) {
                    case 'TRADEPRICE': setPriceTick(data as TradePriceTickMessage); break
                    case 'BIDASKPRICE': setOrderbook(data as BidAskPriceTickMessage); break
                    case 'DETAIL': setDetailTick(data as StockDetailTickMessage); break
                }
            })

            setConnected(true)
            return () => sub.unsubscribe()
        })

        return () => stop()
    }, [selectedStock?.code])

    return (
        <RealtimeContext.Provider value={{ priceTick, orderbook, detailTick, connected }}>
            {children}
        </RealtimeContext.Provider>
    )
}

export function useRealtime() {
    const ctx = useContext(RealtimeContext)
    if (!ctx) throw new Error("useRealtime must be used within RealtimeProvider")
    return ctx
}
