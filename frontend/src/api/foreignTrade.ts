import axios from './axios'

export interface ForeignInstitutionTrade {
    date: string
    closePrice: string
    diff: string
    rate: string
    volume: string
    institutionNetBuy: string
    foreignNetBuy: string
    foreignHoldings: string
    foreignRate: string
}

export interface TradePageResponse {
    items: ForeignInstitutionTrade[]
    hasNext: boolean
}

export const getForeignTrades = (code: string, page: number) =>
    axios.get<TradePageResponse>(`/stocks/${code}/foreign`, { params: { page } }).then(r => r.data)
