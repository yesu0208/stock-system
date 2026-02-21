export interface TradePriceTickMessage {
    tickMessageType: 'TRADEPRICE'
    stockCode: string
    tradeTime: string
    curPrice: number
    prevCloseDiff: number
    startPrice: number
    highPrice: number
    lowPrice: number
    tradingVolumeTick: number
    totalTradingVolume: number
    totalTradingValue: number
    totalSellVolume: number
    totalBuyVolume: number
    tradingType: string
    prevDaySameTimeAccVolume: number
    prevClosePrice: number
}
