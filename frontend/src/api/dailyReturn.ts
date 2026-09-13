import instance from './axios'
import type { DailyReturnHistoryResponse } from '../types/dailyReturn'

export async function getDailyReturnHistory(page: number, size: number): Promise<DailyReturnHistoryResponse> {
    const res = await instance.get<DailyReturnHistoryResponse>('/users/returns/history', {
        params: { page, size },
    })
    return res.data
}
