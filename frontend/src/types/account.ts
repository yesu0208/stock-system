export interface StockInfo {
    quantity: number
    buyPrice: number
    availableQuantity: number
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
}
