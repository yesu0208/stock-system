export type DealRankMarket = 'KOSPI' | 'KOSDAQ'
export type InvestorType = 'FOREIGN' | 'INSTITUTION'
export type DealType = 'BUY' | 'SELL'
export type PeriodType = 'DAY' | 'WEEK' | 'MONTH' | 'THREE_MONTH'

export interface PopularStock {
    rank: number
    code: string
    name: string
    price: string
    direction: string
}

export interface UpjongInfo {
    name: string
    no: string
    changeRate: string
    total: string
    rise: string
    steady: string
    fall: string
    graphRatio: string
}

export interface UpjongResponse {
    items: UpjongInfo[]
}

export interface InvestorTrendDto {
    dateOrTime: string
    individual: number
    foreigner: number
    institution: number
    financeInvestment: number
    insurance: number
    fund: number
    bank: number
    etcFinance: number
    pension: number
    corporation: number
}

export interface TrendResponse {
    data: InvestorTrendDto[]
    hasNext: boolean
}

export interface DealRankItem {
    rank: number
    stockCode: string
    stockName: string
    quantity: number
    amount: number
    volume: number
}

export interface DealRankDay {
    dealDate: string
    items: DealRankItem[]
}

export interface DealRankResponse {
    market: string
    investorType: string
    dealType: string
    periodType: string
    days: DealRankDay[]
}
