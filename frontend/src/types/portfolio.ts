export interface PortfolioStockItem {
    stockCode: string
    spotAmount: number
    leverageAmount: number
    totalAmount: number
    ratioInSector: number
    ratioInTotal: number
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
