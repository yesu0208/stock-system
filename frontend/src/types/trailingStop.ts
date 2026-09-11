export type TrailingStopType = 'BUY' | 'SELL'
export type LeverageRatio = 'SPOT' | 'X1_5' | 'X2' | 'X2_5'

export interface TrailingStopRequest {
    stockCode: string
    trailingStopType: TrailingStopType
    orderQuantity: number
    stopPercent: number
    basePrice: number
    leverageRatio?: LeverageRatio | null
}

export interface TrailingStopResponse extends TrailingStopRequest {
    username: string
}

export interface TrailingStopResultResponse {
    responseType: 'SUCCESS' | 'ERROR'
    trailingStopId: number | null
    username: string
    stockCode: string
    trailingStopType: TrailingStopType
    leverageRatio: LeverageRatio | null
    orderQuantity: number
    stopPercent: number
    basePrice: number
    triggerPrice: number | null
    orderTime: string
    errorMessage: string | null
}

export interface TrailingStopResponseMessage {
    trailingStopId: number
    username: string
    stockCode: string
    trailingStopType: TrailingStopType
    leverageRatio: LeverageRatio | null
    orderQuantity: number
    stopPercent: number
    basePrice: number
    triggerPrice: number | null
    orderTime: string
}

export interface TrailingStopCancelRequest {
    trailingStopId: number
    stockCode: string
}

export interface TrailingStopCancelResponse {
    trailingStopId: number
    stockCode: string
}

export interface TrailingStopCancelResultResponse {
    responseType: 'SUCCESS' | 'ERROR'
    trailingStopId: number
    username: string
    stockCode: string
    trailingStopType: TrailingStopType
    triggerPrice: number
    orderQuantity: number
    errorMessage: string | null
}
