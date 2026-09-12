export type OrderType = 'BUY' | 'SELL'
export type LeverageRatio = 'SPOT' | 'X1_5' | 'X2' | 'X2_5'

export interface OrderResultResponse {
    responseType: 'SUCCESS' | 'ERROR'
    orderId: number | null
    username: string
    stockCode: string
    orderType: OrderType
    leverageRatio: LeverageRatio | null
    orderPrice: number
    orderQuantity: number
    orderTime: string | null
    errorMessage: string | null
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
}

export interface OrderResponse {
    username: string;
    stockCode: string;
    orderType: OrderType;
    orderPrice: number;
    orderQuantity: number;
    leverageRatio: LeverageRatio | null;
}
