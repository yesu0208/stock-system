import instance from './axios'
import type { MarketHolidayCreateRequest, MarketHolidayResponse } from '../types/marketHoliday'

export async function getMarketHolidaysAdmin(): Promise<MarketHolidayResponse[]> {
    const res = await instance.get<MarketHolidayResponse[]>('/admin/market/holidays')
    return res.data
}

export async function addMarketHolidayAdmin(req: MarketHolidayCreateRequest): Promise<MarketHolidayResponse> {
    const res = await instance.post<MarketHolidayResponse>('/admin/market/holidays', req)
    return res.data
}

export async function removeMarketHolidayAdmin(holidayDate: string): Promise<void> {
    await instance.delete(`/admin/market/holidays/${holidayDate}`)
}
