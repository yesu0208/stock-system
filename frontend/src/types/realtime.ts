export interface PriceLevel {
    price: number
    quantity: number
}

export interface BidAskPriceTickMessage {
    tickMessageType: 'BIDASKPRICE'
    stockCode: string
    asks: PriceLevel[]
    bids: PriceLevel[]
    totalAskNum: number
    totalBidNum: number
}

export interface TradePriceTickMessage {
    tickMessageType: 'TRADEPRICE'
    stockCode: string
    tradeTime: string
    curPrice: number
    prevCloseDiff: number
    prevClosePrice: number
    prevCloseRate: string
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
}

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
