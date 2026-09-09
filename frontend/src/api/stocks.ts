import axios from './axios'
import type { StockInfoResponse } from '../types/stockInfo'
import type { StockDetailExtraResponse } from '../types/stockDetail'

export const getStockInfo = (code: string) =>
    axios.get<StockInfoResponse>(`/stocks/${code}`).then(r => r.data)

export const getStockDetailExtra = (code: string) =>
    axios.get<StockDetailExtraResponse>(`/stocks/${code}/detail-extra`).then(r => r.data)
