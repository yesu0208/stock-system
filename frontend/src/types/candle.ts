export type TickMessageType =
    | 'TRADEPRICE'
    | 'BIDASKPRICE'
    | 'DETAIL'
    | 'DAILY_CHART_SNAPSHOT'
    | 'MINUTE_CHART_SNAPSHOT'
    | 'DAILY_CANDLE'
    | 'MINUTE_CANDLE'

// 분봉
export interface MinuteCandle {
    date: string
    time: string
    open: number
    high: number
    low: number
    close: number
    volume: number
}

export interface MinuteChartSnapshotMessage {
    tickMessageType: 'MINUTE_CHART_SNAPSHOT'
    stockCode: string
    candles: MinuteCandle[]
}

export interface MinuteCandleTickMessage {
    tickMessageType: 'MINUTE_CANDLE'
    stockCode: string
    candle: MinuteCandle
}

// 일봉
export interface CandleData {
    date: string
    open: number
    high: number
    low: number
    close: number
    volume: number
}

export interface DailyChartSnapshotMessage {
    tickMessageType: 'DAILY_CHART_SNAPSHOT'
    stockCode: string
    candles: CandleData[]
}

export interface DailyCandleTickMessage {
    tickMessageType: 'DAILY_CANDLE'
    stockCode: string
    candle: CandleData
}
