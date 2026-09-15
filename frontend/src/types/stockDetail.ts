export interface StockDetailTickMessage {
    tickMessageType: 'DETAIL'
    stockCode: string
    stockName: string
    market: string
    baseTime: string
    currentPrice: string
    diffPrice: string
    diffRate: string
    direction: string
    prevPrice: string
    openPrice: string
    highPrice: string
    upperLimit: string
    lowPrice: string
    lowerLimit: string
    volume: string
    tradingValue: string
}
