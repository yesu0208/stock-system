export interface MarketBreadth {
    upperLimit: string
    rise: string
    steady: string
    fall: string
    lowerLimit: string
    basis: string
}

export interface ProgramTrade {
    arbitrage: string
    nonArbitrage: string
    total: string
}

export interface InvestorTrend {
    personal: string
    foreigner: string
    institution: string
}

export interface MarketIndexInfo {
    name: string
    currentIndex: string
    changeValue: string
    changeRate: string
    direction: string
    baseTime: string
    breadth: MarketBreadth
    programTrade: ProgramTrade
    investorTrend: InvestorTrend
}

export interface MarketMainResponse {
    kospi: MarketIndexInfo
    kosdaq: MarketIndexInfo
    kospi200: MarketIndexInfo
}
