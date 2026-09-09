import axios from './axios'

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

export interface UpjongStock {
    name: string
    code: string
    price: string
    change: string
    rate: string
}

export const getAllUpjongs = () =>
    axios.get<{ items: UpjongInfo[] }>('/stocks/market/upjong').then(r => r.data.items)

export const getUpjongStocks = (no: string) =>
    axios.get<{ upjongName: string; items: UpjongStock[] }>(`/stocks/upjong/${no}`).then(r => r.data.items)
