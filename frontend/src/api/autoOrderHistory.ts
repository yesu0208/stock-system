import api from '../lib/api'
import type { HistoryPageResponse, AutoOrderHistoryItem } from '../types/history'

interface HistoryParams {
    stockCode?: string
    from?: string
    to?: string
    page?: number
    size?: number
}

export async function getAutoOrderHistory(params: HistoryParams = {}): Promise<HistoryPageResponse<AutoOrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<AutoOrderHistoryItem>>('/auto-orders/history', { params })
    return res.data
}

export async function getAutoOrderCancelHistory(params: HistoryParams = {}): Promise<HistoryPageResponse<AutoOrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<AutoOrderHistoryItem>>('/auto-orders/cancels', { params })
    return res.data
}

export async function getAutoOrderUnfilled(params: HistoryParams = {}): Promise<HistoryPageResponse<AutoOrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<AutoOrderHistoryItem>>('/auto-orders/unfilled', { params })
    return res.data
}

export async function getAutoOrderTriggered(params: HistoryParams = {}): Promise<HistoryPageResponse<AutoOrderHistoryItem>> {
    const res = await api.get<HistoryPageResponse<AutoOrderHistoryItem>>('/auto-orders/triggered', { params })
    return res.data
}

export async function cancelAutoOrder(autoOrderId: number, stockCode: string): Promise<void> {
    await api.post('/auto-orders/cancel', { autoOrderId, stockCode })
}
