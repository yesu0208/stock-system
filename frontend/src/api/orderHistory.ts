import api from '../lib/api'
import type { HistoryPageResponse, OrderHistoryItem, TradeHistoryItem } from '../types/history'

interface HistoryParams {
    stockCode?: string
    from?: string // ISO 8601
    to?: string
    page?: number
    size?: number
}

export async function getOrderHistory(params: HistoryParams = {}): Promise<HistoryPageResponse<OrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<OrderHistoryItem>>('/orders/history', { params })
    return res.data
}

export async function getOrderCancelHistory(params: HistoryParams = {}): Promise<HistoryPageResponse<OrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<OrderHistoryItem>>('/orders/cancels', { params })
    return res.data
}

export async function getUnfilledOrders(params: HistoryParams = {}): Promise<HistoryPageResponse<OrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<OrderHistoryItem>>('/orders/unfilled', { params })
    return res.data
}

export async function getTradeHistory(params: HistoryParams = {}): Promise<HistoryPageResponse<TradeHistoryItem>> {
    const res = await api.get<HistoryPageResponse<TradeHistoryItem>>('/orders/trades', { params })
    return res.data
}

export async function cancelOrder(orderId: number, stockCode: string): Promise<void> {
    await api.post('/cancels', { orderId, stockCode })
}
