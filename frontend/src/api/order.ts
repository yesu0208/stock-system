import api from '../lib/api'
import type { OrderResponse, OrderType, LeverageRatio, OrderExecutionType } from '../types/order'

export interface PlaceOrderRequest {
    stockCode: string
    orderType: OrderType
    orderPrice: number
    orderQuantity: number
    leverageRatio: LeverageRatio | null
    orderExecutionType: OrderExecutionType
}

export async function placeOrder(req: PlaceOrderRequest): Promise<OrderResponse> {
    const res = await api.post<OrderResponse>('/orders', req)
    return res.data
}
