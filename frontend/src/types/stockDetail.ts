export interface BrokerTradeInfo {
    sellBroker: string
    sellVolume: string
    buyBroker: string
    buyVolume: string
    sellBrokerClass: string
    sellVolumeClass: string
    buyBrokerClass: string
    buyVolumeClass: string
}

export interface ForeignBrokerSummary {
    name: string
    sellVolume: string
    buyDiff: string
    buyVolume: string
    sellClass: string
    buyDiffClass: string
    buyVolumeClass: string
}

export interface StockDetailExtraResponse {
    stockCode: string
    companySummary: string
    warningType: string
    manage: string
    brokerTrades: BrokerTradeInfo[]
    foreignBrokerSummary: ForeignBrokerSummary | null
}
