import instance from './axios'

export interface StockDetailExtraResponse {
    stockCode: string
    market: string
    companySummary: string
    warningType: string
    manage: string
    brokerTrades: unknown[]
    foreignBrokerSummary: unknown
}

export async function getStockDetailExtra(code: string): Promise<StockDetailExtraResponse> {
    const res = await instance.get<StockDetailExtraResponse>(`/stocks/${code}/detail-extra`)
    return res.data
}
