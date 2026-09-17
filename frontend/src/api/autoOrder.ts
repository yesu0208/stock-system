import api from '../lib/api'
import type { AutoOrderResponse } from '../types/autoOrder'
import type { LeverageRatio } from '../types/order'

export interface PlaceAutoOrderRequest {
    stockCode: string
    autoOrderType: 'BUY' | 'SELL'
    triggerPrice: number
    orderPrice: number
    orderQuantity: number
    leverageRatio: LeverageRatio | null
}

export async function placeAutoOrder(req: PlaceAutoOrderRequest): Promise<AutoOrderResponse> {
    const res = await api.post<AutoOrderResponse>('/auto-orders', req)
    return res.data
}
