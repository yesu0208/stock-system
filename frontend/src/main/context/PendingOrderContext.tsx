import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import { stockNameMap } from '../../constants/stocks'
import type { OrderResponseMessage, OrderOrigin } from '../../types/order'
import type { AutoOrderResponseMessage } from '../../types/autoOrder'

export interface PendingOrder {
    orderId: string
    rawId: number
    isAuto: boolean
    stockCode: string
    stockName: string
    side: 'BUY' | 'SELL'
    orderType: 'MARKET' | 'LIMIT' | 'CONDITIONAL'
    price: number
    triggerPrice?: number
    orderQty: number
    remainingQty: number
    leverage: number
    time: string
    origin?: OrderOrigin
}

function leverageRatioToNumber(ratio: string | null): number {
    switch (ratio) {
        case 'X1_5': return 1.5
        case 'X2':   return 2
        case 'X2_5': return 2.5
        default:     return 1
    }
}

interface PendingOrderContextValue {
    pendingOrders: PendingOrder[]
}

const PendingOrderContext = createContext<PendingOrderContextValue | null>(null)

export function PendingOrderProvider({ children }: { children: ReactNode }) {
    const { subscribeDestination } = useRealtime()

    const [orders, setOrders] = useState<OrderResponseMessage[]>([])
    const [autoOrders, setAutoOrders] = useState<AutoOrderResponseMessage[]>([])

    useEffect(() => {
        return subscribeDestination('/user/sub/order', (data: OrderResponseMessage[]) => {
            setOrders(data)
        })
    }, [subscribeDestination])

    useEffect(() => {
        return subscribeDestination('/user/sub/auto/order', (data: AutoOrderResponseMessage[]) => {
            setAutoOrders(data)
        })
    }, [subscribeDestination])

    const pendingFromOrders: PendingOrder[] = orders
        .filter((o) => o.remainingQuantity > 0)
        .map((o) => ({
            orderId: `order-${o.orderId}`,
            rawId: o.orderId,
            isAuto: false,
            stockCode: o.stockCode,
            stockName: stockNameMap[o.stockCode] ?? o.stockCode,
            side: o.orderType,
            orderType: o.orderExecutionType,
            price: o.orderPrice,
            orderQty: o.orderQuantity,
            remainingQty: o.remainingQuantity,
            leverage: leverageRatioToNumber(o.leverageRatio),
            time: o.orderTime,
            origin: o.origin,
        }))

    const pendingFromAutoOrders: PendingOrder[] = autoOrders
        .map((o) => ({
            orderId: `auto-${o.autoOrderId}`,
            rawId: o.autoOrderId,
            isAuto: true,
            stockCode: o.stockCode,
            stockName: stockNameMap[o.stockCode] ?? o.stockCode,
            side: o.autoOrderType,
            orderType: 'CONDITIONAL' as const,
            price: o.orderPrice,
            triggerPrice: o.triggerPrice,
            orderQty: o.orderQuantity,
            remainingQty: o.orderQuantity,
            leverage: leverageRatioToNumber(o.leverageRatio),
            time: o.orderTime,
        }))

    const pendingOrders = [...pendingFromOrders, ...pendingFromAutoOrders].sort(
        (a, b) => new Date(b.time).getTime() - new Date(a.time).getTime()
    )

    return (
        <PendingOrderContext.Provider value={{ pendingOrders }}>
            {children}
        </PendingOrderContext.Provider>
    )
}

export function usePendingOrders() {
    const ctx = useContext(PendingOrderContext)
    if (!ctx) throw new Error('usePendingOrders는 PendingOrderProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
