export interface DailyReturnHistoryItem {
    date: string
    previousDayTotalAsset: number
    totalAsset: number
    dailyProfitAmount: number
    dailyProfitRate: number
    dailyTradeAmount: number
    cumulativeProfitAmount: number
    cumulativeProfitRate: number
}

export interface DailyReturnHistoryResponse {
    items: DailyReturnHistoryItem[]
    hasNext: boolean
}
