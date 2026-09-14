import instance from './axios'
import type {
    PopularStock,
    UpjongResponse,
    TrendResponse,
    UpjongStockResponse,
    DealRankResponse,
    DealRankMarket,
    InvestorType,
    DealType,
    PeriodType,
} from '../types/marketWidgets'

export type InvestorTrendMarket = 'KOSPI' | 'KOSDAQ' | 'FUTURES'

const MARKET_CODE: Record<InvestorTrendMarket, string> = {
    KOSPI: 'KOSPI',
    KOSDAQ: 'KOSDAQ',
    FUTURES: 'FUTURES',
}

export async function getPopularStocks(): Promise<PopularStock[]> {
    const res = await instance.get<PopularStock[]>('/stocks/market/popular')
    return res.data
}

export async function getUpjongs(): Promise<UpjongResponse> {
    const res = await instance.get<UpjongResponse>('/stocks/market/upjong')
    return res.data
}

export async function getUpjongStocks(no: string): Promise<UpjongStockResponse> {
    const res = await instance.get<UpjongStockResponse>(`/stocks/upjong/${no}`)
    return res.data
}

export async function getInvestorTrend(
    market: InvestorTrendMarket,
    type: 'time' | 'day',
    page = 1
): Promise<TrendResponse> {
    const res = await instance.get<TrendResponse>(`/stocks/investor-trend/${type}`, {
        params: { market: MARKET_CODE[market], page },
    })
    return res.data
}

export async function getDealRank(
    market: DealRankMarket,
    investorType: InvestorType,
    dealType: DealType,
    periodType: PeriodType = 'DAY'
): Promise<DealRankResponse> {
    const res = await instance.get<DealRankResponse>('/stocks/deal-rank', {
        params: { market, investorType, dealType, periodType },
    })
    return res.data
}
