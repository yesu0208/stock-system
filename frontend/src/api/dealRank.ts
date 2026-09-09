import axios from './axios'

export interface DealRankItem {
    rank: number
    stockCode: string | null
    stockName: string
    quantity: number | null
    amount: number | null
    volume: number | null
}

export interface DealRankDay {
    dealDate: string
    items: DealRankItem[]
}

export interface DealRankResponse {
    market: string
    investorType: string
    dealType: string
    days: DealRankDay[]
}

export const getDealRank = (
    market: 'KOSPI' | 'KOSDAQ',
    investorType: 'FOREIGN' | 'INSTITUTION',
    dealType: 'BUY' | 'SELL'
) =>
    axios
        .get<DealRankResponse>('/stocks/deal-rank', { params: { market, investorType, dealType } })
        .then(r => r.data)