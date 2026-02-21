export type AutoOrderType = 'BUY' | 'SELL'
export type ResponseType = 'SUCCESS' | 'ERROR'

export interface AutoOrderResultResponse {
    responseType: ResponseType
    autoOrderId: number | null
    username: string
    stockCode: string
    autoOrderType: AutoOrderType
    triggerPrice: number
    orderPrice: number
    orderQuantity: number
    orderTime?: string
    errorMessage?: string | null
}

export interface AutoOrderResponseMessage {
    autoOrderId: number
    stockCode: string
    autoOrderType: AutoOrderType
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
}
