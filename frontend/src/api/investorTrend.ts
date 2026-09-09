import axios from './axios'

export type InvestorTrendDto = {
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
