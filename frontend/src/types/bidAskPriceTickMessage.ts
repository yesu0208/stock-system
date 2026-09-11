export interface PriceLevel {
    price: number
    quantity: number
}

export interface BidAskPriceTickMessage {
    tickMessageType: 'BIDASKPRICE'
    stockCode: string
    asks: PriceLevel[]
    bids: PriceLevel[]
}
