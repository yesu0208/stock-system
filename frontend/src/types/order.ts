export type OrderType = 'BUY' | 'SELL'
export type LeverageRatio = 'SPOT' | 'X1_5' | 'X2' | 'X2_5'
export type OrderExecutionType = 'MARKET' | 'LIMIT'

export type OrderOrigin =
    | 'MANUAL'
    | 'AUTO_ORDER'
    | 'OTOCO_ENTRY'
    | 'OTOCO_TAKE_PROFIT'
    | 'OTOCO_STOP_LOSS'
    | 'TRAILING_STOP'

export interface OrderResponseMessage {
    orderId: number
    username: string
    stockCode: string
    orderType: OrderType
    leverageRatio: LeverageRatio | null
    orderPrice: number
    orderQuantity: number
    remainingQuantity: number
    orderTime: string
    orderExecutionType: OrderExecutionType
    origin: OrderOrigin
    originId: number | null
}

export interface OrderResponseMessage {
    orderId: number
    username: string
    stockCode: string
    orderType: OrderType
    leverageRatio: LeverageRatio | null
    orderPrice: number
    orderQuantity: number
    remainingQuantity: number
    orderTime: string
    orderExecutionType: OrderExecutionType
}

export interface OrderResponse {
    username: string;
    stockCode: string;
    orderType: OrderType;
    orderPrice: number;
    orderQuantity: number;
    leverageRatio: LeverageRatio | null;
}
