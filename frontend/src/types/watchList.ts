export interface WatchListItem {
    stockCode: string
    stockName: string
    sortOrder: number | null
}

export interface AddWatchListRequest {
    stockCode: string
    stockName: string
}
