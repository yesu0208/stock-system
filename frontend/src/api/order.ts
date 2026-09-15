import api from '../lib/api'
import type { OrderResponse, OrderType, LeverageRatio } from '../types/order'

export interface PlaceOrderRequest {
    stockCode: string
    orderType: OrderType
    orderPrice: number
    orderQuantity: number
    leverageRatio: LeverageRatio | null
}

export async function placeOrder(req: PlaceOrderRequest): Promise<OrderResponse> {
    const res = await api.post<OrderResponse>('/orders', req)
    return res.data
}
