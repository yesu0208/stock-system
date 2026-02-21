export type TradeResponse = {
    tradeId: number
    orderId: number
    username: string
    stockCode: string
    tradeType: 'BUY' | 'SELL'
    tradePrice: number
    tradeQuantity: number
    executedAt: string
}
