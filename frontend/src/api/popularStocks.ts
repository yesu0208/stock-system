import axios from './axios'

export interface PopularStock {
    rank: number
    code: string
    name: string
    price: string
    direction: string
}

export const getPopularStocks = () =>
    axios.get<PopularStock[]>('/stocks/market/popular').then(r => r.data)
