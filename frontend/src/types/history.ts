export interface HistoryPageResponse<T> {
    items: T[]
    page: number
    size: number
    totalElements: number
    hasNext: boolean
}

export type OrderStatus = 'OPEN' | 'PARTIAL' | 'FILLED' | 'CANCELED'

export type OrderOrigin =
    | 'MANUAL'
    | 'AUTO_ORDER'
    | 'OTOCO_ENTRY'
    | 'OTOCO_TAKE_PROFIT'
    | 'OTOCO_STOP_LOSS'
    | 'TRAILING_STOP'

export interface OrderHistoryItem {
    orderId: number
    stockCode: string
    orderType: 'BUY' | 'SELL'
    leverageRatio: 'SPOT' | 'X1_5' | 'X2' | 'X2_5' | null
    orderPrice: number
    orderQuantity: number
    remainingQuantity: number
    orderStatus: OrderStatus
    orderTime: string
    notionalValue: number
    initialMargin: number | null
    maintenanceMarginRate: number | null
    liquidationPrice: number | null
    orderExecutionType: 'MARKET' | 'LIMIT'
    origin: OrderOrigin
    originId: number | null
}

export interface TradeHistoryItem {
    tradeId: number
    orderId: number
    stockCode: string
    tradeType: 'BUY' | 'SELL'
    tradePrice: number
    tradeQuantity: number
    executedAt: string
    leverageRatio: 'SPOT' | 'X1_5' | 'X2' | 'X2_5' | null
    notionalValue: number
    initialMargin: number | null
    maintenanceMarginRate: number | null
    liquidationPrice: number | null
    origin: OrderOrigin
    originId: number | null
}

export type AutoOrderStatus = 'ACTIVE' | 'TRIGGERED' | 'CANCELED'

export interface AutoOrderHistoryItem {
    autoOrderId: number
    stockCode: string
    autoOrderType: 'BUY' | 'SELL'
    leverageRatio: 'SPOT' | 'X1_5' | 'X2' | 'X2_5' | null
    triggerPrice: number
    orderPrice: number
    orderQuantity: number
    autoOrderStatus: AutoOrderStatus
    orderTime: string
    notionalValue: number
    initialMargin: number | null
    maintenanceMarginRate: number | null
    liquidationPrice: number | null
}
