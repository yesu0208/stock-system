import instance from './axios'
import type { AlertDirection } from '../types/alert'

/**
 * 트레이딩 액션(주문/취소/OTOCO 등)이 아니라 알림 등록/해지이므로
 * lib/api.ts가 아니라 axios.ts 계열로 분류
 */

export interface AlertRequest {
    stockCode: string
    direction: AlertDirection
    triggerPrice: number
}

export interface AlertResponse extends AlertRequest {
    alertId: number
    username: string
}

export async function createAlert(req: AlertRequest): Promise<AlertResponse> {
    const res = await instance.post<AlertResponse>('/alerts', req)
    return res.data
}

export async function cancelAlert(alertId: number, stockCode: string): Promise<void> {
    await instance.post('/alerts/cancel', { alertId, stockCode })
}
