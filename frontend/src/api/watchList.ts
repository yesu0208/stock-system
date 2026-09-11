import instance from './axios'
import type { WatchListItem, AddWatchListRequest } from '../types/watchList'

export async function getWatchList(): Promise<WatchListItem[]> {
    const res = await instance.get<WatchListItem[]>('/watchlist')
    return res.data
}

export async function addWatchList(req: AddWatchListRequest): Promise<WatchListItem> {
    const res = await instance.post<WatchListItem>('/watchlist', req)
    return res.data
}

export async function removeWatchList(stockCode: string): Promise<void> {
    await instance.delete(`/watchlist/${stockCode}`)
}
