export interface StockInfoResponse {
    marketCap: string
    marketCapRank: string
    listedShares: string

    parValue: string
    tradingUnit: string

    foreignLimit: string
    foreignOwned: string
    foreignRate: string

    opinion: string
    targetPrice: string

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
