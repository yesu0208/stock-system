import instance from './axios'
import type {
    PopularStock,
    UpjongResponse,
    TrendResponse,
    DealRankResponse,
    DealRankMarket,
    InvestorType,
    DealType,
    PeriodType,
} from '../types/marketWidgets'

export async function getPopularStocks(): Promise<PopularStock[]> {
    const res = await instance.get<PopularStock[]>('/stocks/market/popular')
    return res.data
}

export async function getUpjongs(): Promise<UpjongResponse> {
    const res = await instance.get<UpjongResponse>('/stocks/market/upjong')
    return res.data
}

export async function getInvestorTrend(
    market: 'KOSPI' | 'KOSDAQ',
    type: 'time' | 'day',
    page = 1
): Promise<TrendResponse> {
    const res = await instance.get<TrendResponse>(`/stocks/investor-trend/${type}`, {
        params: { market, page },
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
