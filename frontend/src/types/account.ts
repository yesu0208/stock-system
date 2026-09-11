export interface StockInfo {
    quantity: number
    buyPrice: number
    availableQuantity: number
}

export interface LeveragePositionView {
    stockCode: string
    leverageRatio: 'SPOT' | 'X1_5' | 'X2' | 'X2_5'
    quantity: number
    availableQuantity: number
    purchaseAmount: number
    loanAmount: number
    evaluationAmount: number
    netValue: number
    profitRate: number
    currentPrice: number
}

export interface AccountResponse {
    username: string
    totalValue: number
    totalCash: number
    availableCash: number
    reservedCash: number
    stockValue: number
    buyValue: number
    totalProfit: number
    totalProfitRate: number
    accumulatedProfit: number
    accumulatedProfitRate: number
    stocks: Record<string, StockInfo>
    profitRates: Record<string, number>
    profitAmounts: Record<string, number>
    currentPrices: Record<string, number>
    leverageNetValue: number | null
    leverageLoanTotal: number | null
    leveragePositions: LeveragePositionView[]
}
