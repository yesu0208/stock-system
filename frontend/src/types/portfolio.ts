export interface PortfolioStockItem {
    stockCode: string
    spotAmount: number
    leverageAmount: number
    totalAmount: number
    ratioInSector: number
    ratioInTotal: number
    profitAmount: number
    profitRate: number
    spotBuyAmount: number
    spotProfitAmount: number
    spotProfitRate: number
    leverageBuyAmount: number
    leverageEquityAmount: number
    leverageProfitAmount: number
    leverageProfitRate: number
}

export interface PortfolioSectorItem {
    sector: string
    evaluationAmount: number
    ratioInTotal: number
    stocks: PortfolioStockItem[]
}

export interface PortfolioResponse {
    username: string
    totalAssetValue: number
    cashValue: number
    cashRatio: number
    sectors: PortfolioSectorItem[]
}
