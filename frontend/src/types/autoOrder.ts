export type AutoOrderType = 'BUY' | 'SELL'
export type ResponseType = 'SUCCESS' | 'ERROR'
export type LeverageRatio = 'SPOT' | 'X1_5' | 'X2' | 'X2_5'

export interface AutoOrderResultResponse {
    responseType: ResponseType
    autoOrderId: number | null
    username: string
    stockCode: string
    autoOrderType: AutoOrderType
    leverageRatio: LeverageRatio | null
    triggerPrice: number
    orderPrice: number
    orderQuantity: number
    orderTime?: string
    errorMessage?: string | null
    resultCode?: string | null
}

export interface AutoOrderResponseMessage {
    autoOrderId: number
    stockCode: string
    autoOrderType: AutoOrderType
    leverageRatio: LeverageRatio | null
    triggerPrice: number
    orderPrice: number
    orderQuantity: number
    orderTime: string
}

export interface AutoOrderResponse {
    username: string;
    stockCode: string;
    autoOrderType: AutoOrderType;
    triggerPrice: number;
    orderPrice: number;
    orderQuantity: number;
    leverageRatio: LeverageRatio | null;
}
