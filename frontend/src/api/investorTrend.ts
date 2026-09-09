import axios from './axios'
import type { InvestorTrendDto } from '../types/investorTrend'

export const getInvestorTrend = (
    market: 'KOSPI' | 'KOSDAQ' | 'FUTURES',
    type: 'time' | 'day',
    page: number
) =>
    axios
        .get<{ data: InvestorTrendDto[]; hasNext: boolean }>(`/stocks/investor-trend/${type}`, {
            params: { market, page },
        })
        .then(r => r.data)
