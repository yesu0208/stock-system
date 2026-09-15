import instance from './axios'

export interface BrokerTradeInfo {
    sellBroker: string
    sellBrokerClass: string
    sellVolume: string
    sellVolumeClass: string
    buyBroker: string
    buyBrokerClass: string
    buyVolume: string
    buyVolumeClass: string
}

export interface ForeignBrokerSummary {
    sellVolume: string
    buyVolume: string
    buyDiff: string
}

export interface StockDetailExtraResponse {
    stockCode: string
    market: string
    companySummary: string
    warningType: string
    manage: string
    brokerTrades: BrokerTradeInfo[]
    foreignBrokerSummary: ForeignBrokerSummary | null
}

export async function getStockDetailExtra(code: string): Promise<StockDetailExtraResponse> {
    const res = await instance.get<StockDetailExtraResponse>(`/stocks/${code}/detail-extra`)
    return res.data
}

export interface StockInfo {
    marketCap: string
    listedShares: string
    parValue: string
    tradingUnit: string
    foreignLimit: string
    foreignOwned: string
    foreignRate: string
    opinion: string
    targetPrice: string
    consensusDate: string
    high52: string
    low52: string
    per: string
    eps: string
    estimatedPer: string
    estimatedEps: string
    pbr: string
    bps: string
    dividendYield: string
    sameIndustryPer: string
    sameIndustryRate: string
}

export async function getStockInfo(code: string): Promise<StockInfo> {
    const res = await instance.get<StockInfo>(`/stocks/${code}`)
    return res.data
}

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

export async function getForeignTrades(code: string, page: number): Promise<TradePageResponse> {
    const res = await instance.get<TradePageResponse>(`/stocks/${code}/foreign`, {
        params: { page },
    })
    return res.data
}
