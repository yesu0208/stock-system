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
